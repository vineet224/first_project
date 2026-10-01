Context & Objective
Core Project Experience: Production credit card onboarding integration for Bank of Baroda (BoB) at Scapia, built via Vegapay as the bank's Technical Service Provider (TSP).

Prototype Objective: Rebuilding a production-accurate prototype to clearly articulate, defend, and demonstrate the architectural trade-offs, state machines, and resilience patterns in System Design (HLD/LLD) technical interviews.

Dual-Workflow Architecture & Domain Boundary
The onboarding journey is bifurcated into two separate services across distinct execution stages:

Workflow 1: Generic Onboarding (general_workflow_service)

Scope: Captures user profile, performs pre-qualification, and evaluates multi-bank eligibility before handing off to a specific bank's engine.

Steps:

Mobile number input and OTP verification.

Identity capture: Full Name, Date of Birth, and PAN.

PAN verification against NSDL (validating name and DOB match).

Soft Credit Bureau check via Indian credit bureau servers (using user-consented OTP; generates a soft pull that does not count as a hard inquiry on the credit score).

Soft BRE (Business Rules Engine) evaluation across partner banks (e.g., Federal Bank vs. Bank of Baroda).

Bank selection screen presenting qualified cards.

Handoff: Selecting Bank of Baroda terminates general_workflow_service execution and transfers context to bob_onboarding_service.

Workflow 2: Bank of Baroda Engine (bob_onboarding_service)

Scope: Specialized card onboarding engine interfacing directly with Vegapay TSP.

Steps:

Customer registration on Vegapay with consent, IP address, and device footprint.

Dedupe and PAN confirmation on bank core systems.

Current/permanent address and employment data capture.

eKYC via embedded Prefios SDK (direct Aadhaar authentication).

Asynchronous Video KYC (VKYC) through Prefios/Vegapay agent network.

Vegapay triggers bank hard credit inquiry and internal underwriting engine.

Sanction limit generation and offer display.

Offer acceptance (terminal state for this service).

Infrastructure & Deployment Topology
Scapia Cloud (AWS VPC):

Hosts the React Native Mobile Client, API Gateway, and general_workflow_service.

Communicates with public/third-party verification gateways (NSDL, Indian credit bureau soft pull API).

Bank of Baroda Infra (Bank VPC):

Hosts bob_onboarding_service (written by Scapia engineers, deployed inside the bank's private perimeter).

Hosts Vegapay TSP core engine, internal bank Redis clusters, and internal relational databases.

All communications between bob_onboarding_service and Vegapay TSP remain intra-VPC over internal banking subnets.

Identity, Idempotency & Data Modeling
Identity Hierarchy:

A single mobile number maps strictly to a single, immutable customerId for lifetime tracking.

A customerId can have multiple expired, rejected, or discarded workflows over time (e.g., 30-day re-application window), but strictly one active (pending or in-progress) or completed workflow at any moment.

A single active general_workflow_id maps 1:1 to an initiated bob_onboarding_workflow_id.

Polymorphic Step Storage:

Step submissions do not require separate database tables per form.

State transitions persist user input polymorphically as: { workflow_id, state_name, payload_json }.

In the Java/Spring backend, payloads are deserialized and strictly validated as typed domain records (e.g., PanVerificationPayload, AddressKycPayload) before database persistence.

Security, Regulatory & PCI Scope Boundaries
No PCI-DSS Scope:

The service boundaries strictly terminate at Limit Sanction and Offer Acceptance.

Card embossing, PAN/CVV generation, and virtual card activation are handled by dedicated downstream card management services, keeping this onboarding engine out of PCI-DSS scope.

Zero Plain-Text Aadhaar Footprint:

Aadhaar details are submitted directly through the client-side Prefios SDK to partner verification servers.

The Scapia backend never handles, inspects, or logs raw Aadhaar numbers, storing only transaction reference IDs and verification status.

Audit & Regulatory Compliance:

Immutable audit logging captures timestamped customer consent, IP address, and device fingerprint at registration, which is forwarded directly to Vegapay for RBI compliance.

State Machine Pruning & Anti-Corruption Layer (ACL)
Pruned State Projection:

Vegapay TSP operates an asynchronous state machine with 14–20 low-level micro-states (e.g., file generation, internal dedupe steps, intermediate scoring states).

bob_onboarding_service prunes these down to 7–10 actionable domain states, exposing only stages that require user interaction (e.g., address input, VKYC launch, offer approval) or mark explicit milestones.

Anti-Corruption Layer (ACL):

Acts as a translation barrier between Vegapay’s unstable or vendor-specific responses and Scapia’s internal domain model.

Translates TSP micro-states into deterministic internal states (BOB_INITIATED, PAN_VERIFIED, EKYC_PENDING, VKYC_PENDING, UNDERWRITING_IN_PROGRESS, LIMIT_SANCTIONED, OFFER_ACCEPTED).

Synchronization Model & Concurrency Control
Frontend-Driven Just-In-Time (JIT) Hydration:

The primary sync mechanism relies on frontend polling (GET /status and POST /update/status).

External Vegapay status calls are triggered lazily when the active user requests an update, conserving partner API rate limits and preventing idle background polling for dropped-off users.

Active Background Sync Scheduler:

A scheduled background worker monitors long-running asynchronous states (e.g., VKYC_PENDING, bank underwriting) to keep workflow records fresh when the user leaves or closes the app.

Distributed Locking (Redis):

To resolve Dual-Driver Race Conditions between frontend polling (GET /status), user submissions (POST /update/status), and the background sync scheduler, every state operation must acquire a distributed Redis lock (lock:bob:workflow:{workflow_id}).

State modifications require optimistic locking checks (version column) on the database row to prevent lost updates.

Fault Tolerance & Resilience Patterns (Resilience4j)
Rate Limiting: Token Bucket rate limiting applied on all outgoing requests to Vegapay to adhere strictly to agreed bank gateway quotas.

Handling Transient Failures (500s, 502s, Timeouts):

Handled via Resilience4j retries with exponential backoff and randomized jitter to prevent thundering herd spikes against degraded bank infrastructure.

If timeouts persist, the backend returns a safe PROCESSING status to keep the client gracefully waiting instead of terminating the journey.

Handling Deterministic Failures (400, 403, 422):

Instant fail-fast pattern; bypasses retries immediately and maps bank error codes into actionable failure reasons on the UI (e.g., incorrect PAN format).

Circuit Breaker:

Monitors consecutive failures and rate-limit throttles (429s).

Trips to open state when error thresholds are crossed, fast-failing traffic with a cached "System Busy" status to protect upstream bank endpoints.