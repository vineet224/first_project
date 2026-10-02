I’ll compare your model with documented bank and banking-provider APIs, then describe three API styles and two client strategies for each. I’ll focus on workflow execution and recovery, and examine whether “the bank is ahead and our input exists” is enough evidence to advance locally.

Interact with subagent sync_design_review

Start subagent bank_api_examples

Interact with subagent sync_design_review

The public APIs show several valid models. State-gated actions are used in banking systems, but status names vary: Unit’s `Pending` means the provider is processing, whereas your Vegapay contract uses `PENDING` to request client action.

For reconciliation, the crucial question is what a later bank state proves. If its contract guarantees that the required earlier action completed, you can recover local state from that evidence. Stored input alone does not establish completion.

Complete subagent sync_design_review

Complete subagent bank_api_examples

Compact conversation

**Yes—your Vegapay model is a credible way to integrate with a production banking workflow.** Your second design describes a reasonable client architecture for that contract. The important qualification is that banks expose different contracts; the exact `PENDING → IN_PROGRESS → FAILED` meanings are specific to your integration.

Public documentation supports the patterns below, but it cannot establish which approach most banks use or reveal their internal execution engines.

**First, your reconciliation concern**

Under the contract you described, these decisions make sense:

| Vegapay observation | Your backend’s responsibility |
|---|---|
| Current step is `PENDING` | Submit the corresponding action if its input exists and the action is permitted |
| Current step is `IN_PROGRESS` | Wait; do not submit that action again |
| Current step is `FAILED` | Interpret the failure and choose retry, corrected input or another recovery path |
| Vegapay has reached a later milestone | Reconcile local progress using what that milestone guarantees |

The last row needs a more precise explanation than “we had the input, so we assume we called the API.”

**Stored input proves you were ready to submit. Vegapay’s state must provide the evidence that submission was accepted or the required work completed.**

For example, your address response is lost:

1. Both addresses were already persisted locally.
2. The next status response requests the other address.
3. If Vegapay guarantees that this step is reachable only after accepting the earlier address, that observation establishes acceptance.
4. You submit the other saved address and remain `EKYC_PENDING`.
5. You advance beyond EKYC only when the actual EKYC completion boundary is satisfied and the next screen’s required data is saved.

You do not need to prove which network request caused the transition to recover your local screen. You need sufficient evidence that the required business milestone happened for the correct application.

However, if Vegapay can skip that step, restart EKYC, accept input from another channel, or advance without validating the prerequisite, its current state may not prove enough. Then you need a step-result API, submitted-data API, verification reference or operation history.

Also, `IN_PROGRESS` proves processing has started under your contract; it does **not** prove successful completion.

Your interview explanation can therefore be:

> We treat the partner’s workflow as authoritative for bank processing. After an uncertain response, we fetch its current state and reconcile only milestones whose prerequisites are guaranteed by the partner contract. We verify our local prerequisites and retrieve any missing result data before exposing the next screen.

That is a defensible recovery strategy.

One other distinction: rejecting an old-state action does not establish request-key idempotency. **State gating and replayable requests are separate capabilities.** A provider can offer both: Asseco’s banking-platform documentation describes state-based operation rejection and recovery of a lost successful response by repeating the same `X-Request-ID`. [Asseco REST API guide](https://developer.banking.asseco.pl/registry/resource/_system/governance/apimgt/applicationdata/content/instructions/en/REST%20API%20User%20Guide%20%28en%29.pdf)

Below are three API models and two client approaches for each. The provider examples are documented; the client approaches are architectural choices, rather than claims about those providers’ customers.

---

**1. Bank-owned workflow with actions allowed at specific stages**

This is the closest model to your Vegapay integration.

The bank/TSP owns the process and its ordering. Its external API exposes capabilities such as:

- Create an application and obtain its identifier.
- Read the current stage, processing status and failure details.
- Submit stage-specific input or initiate its action.
- Retrieve results such as verification details, URLs or offers.
- Receive completion or information-required notifications, where supported.

The provider decides which action is currently valid. It can reject submissions that conflict with the application’s state.

A concrete onboarding example is Solaris business identification. Clients retrieve required questions, submit answers and documents, then mark the identification ready. That moves legal identification from `information_required` to `pending` for review. Webhooks notify clients of changes, and clients retrieve the identification for details. Notice that Solaris’s `pending` means review is underway—different from your Vegapay meaning. [Solaris business identification](https://docs.solarisgroup.com/guides/kyc/bkyc)

**Combination 1: Stage-gated bank workflow + frontend-triggered backend synchronization**

This resembles your baseline:

- User submission persists input and triggers one immediate progression step.
- Subsequent frontend updates trigger backend status reads.
- The backend selects actions from the current bank state and local prerequisites.
- Lost responses are recovered through status and result retrieval.
- Returning users resume the persisted application.

This can be sufficient when progress only needs to become visible while the user is present, with callbacks covering selected long-running steps. Its limitation is delayed local reconciliation while the user is absent.

The backend still owns state handling: the frontend supplies the trigger, while the backend determines what may happen.

**Combination 2: Stage-gated bank workflow + hybrid synchronization**

This is your second design:

- Frontend updates, workers and webhooks enter the same synchronization policy.
- Workers check eligible pending applications at a suitable cadence.
- Every driver applies the same prerequisite checks, action limits and transition rules.
- Webhooks or status reads recover progress after uncertain responses.

A shared workflow lock prevents overlapping local execution. As you pointed out, sequential drivers may still perform redundant status reads; accepting those reads is reasonable under your stated status-call contract.

Background execution improves recovery and freshness. The bank-state interpretation makes the transitions correct.

---

**2. Asynchronous command API with a queryable operation and replay protection**

Here, the client requests a business operation rather than driving every internal step.

Typical capabilities are:

- Submit an operation, often with a client-generated idempotency key.
- Receive an operation or resulting resource identifier.
- Query processing status and final outcome.
- Receive a completion notification where supported.
- Retrieve the same submission outcome after a lost response using the original key.

The bank executes the operation internally. The client primarily monitors acceptance and completion.

HSBC’s payment APIs illustrate this model: clients submit a domestic payment and query it by payment ID. Its implementation guide explicitly describes recovering from a lost POST response by resubmitting with the same idempotency key. This is a **payment example**, not an assertion about HSBC onboarding. [HSBC implementation guide](https://develop.hsbc.com/sites/default/files/open_banking/HSBC%20UK%20Open%20Banking%20Implementation%20Guide%20%28v3%29.pdf)

An onboarding-related example is Solaris account opening: a request goes through asynchronous checks, with statuses including `INITIATED`, `IN_PROGRESS`, `COMPLETED` and `REJECTED`; a webhook reports completion. [Solaris account opening](https://docs.solarisgroup.com/guides/digital-banking/account-opening)

**Combination 3: Asynchronous operation API + foreground monitoring**

The client backend:

- Saves a stable submission key before sending, when the API supports one.
- Saves the returned operation identifier.
- Checks that operation when the frontend requests an update.
- Recovers an uncertain submission using the documented same-key retry mechanism.
- Advances locally after observing the appropriate outcome.

A timeout does not immediately become a new submission. The backend first resolves the original operation.

Corrected user input represents a new submission where the provider contract allows it; it should not accidentally reuse a key associated with a different payload.

**Combination 4: Asynchronous operation API + durable backend execution**

The backend records work durably, and a worker submits or resumes it:

- A durable task carries the stable submission identity.
- Workers recover uncertain submissions using the provider’s replay contract.
- Webhooks and scheduled checks resolve the operation’s outcome.
- The frontend reads the backend’s persisted progress.

This supports operations that must continue independently of the app session.

Compared with your Vegapay model, recovery can be more explicit: the client tracks a particular operation and its outcome, rather than inferring acceptance from a later workflow stage. This depends on the provider actually offering that capability.

---

**3. Resource and requirements APIs, with the client composing the product journey**

The provider exposes business resources and their status rather than every internal workflow step.

Capabilities may include:

- Create and retrieve an application.
- Inspect missing information or document requirements.
- Upload documents or update permitted application fields.
- Observe approval, rejection or manual review.
- Use resulting customer and account resources for subsequent product steps.
- Subscribe to resource-change events.

The provider still owns its verification and decision-making process. The client owns the broader journey connecting those resources.

Unit, a banking infrastructure provider, documents application states such as `AwaitingDocuments`, `Pending`, `PendingReview`, `Approved` and `Denied`. It explicitly supports monitoring through webhooks or periodic application retrieval. Approval creates an associated customer resource. [Unit applications overview](https://www.unit.co/docs/api/applications/overview/)

**Combination 5: Resource APIs + requirements-driven frontend journey**

The backend translates the provider’s resource status into screens:

- Missing documents → show the required upload screen.
- Processing or manual review → show a waiting screen.
- Approval → save the customer reference and enable the next product step.
- Correctable requirements → collect the specific missing information.

Frontend visits trigger resource refreshes. The client does not need to mirror every hidden provider substep.

**Combination 6: Resource APIs + backend product orchestration**

The backend coordinates dependencies across resources:

- Application approval enables customer-dependent work.
- Customer readiness enables account creation.
- Account completion enables subsequent product setup.
- Workers and events update those facts independently of frontend visits.

Here, progression follows explicit dependencies rather than one numeric ordering of bank states. For example, “application approved and customer ID available” may permit account opening, while another verification remains independently pending.

The client owns the overall product workflow while each provider resource remains authoritative for its own outcome.

---

These models can overlap: a provider may expose resource APIs, enforce stage restrictions and also support idempotency keys.

**For your integration, Combination 2 is a reasonable choice.** You do not need to replace it merely to resemble production systems. The main improvement is to make reconciliation evidence precise:

> We advance because the partner confirms a milestone whose contract establishes the necessary prior work, and we have the data required for the next local state.

If the partner’s status cannot establish that milestone, your backend needs additional result information. Adding more polling cannot resolve that ambiguity.