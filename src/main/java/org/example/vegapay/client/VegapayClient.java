package org.example.vegapay.client;

import org.example.dto.request.AddressRequest;
import org.example.vegapay.request.VegapayCustomerRegistrationRequest;
import org.example.vegapay.response.VegapayCustomerRegistrationResponse;
import org.example.vegapay.response.VegapayGeneralResponse;
import org.example.vegapay.response.VegapayStatusResponse;

public interface VegapayClient {

    /**
     * POST /vegapay/api/customer-registration
     * Registers a new customer with Vegapay.
     *
     * @param request Customer registration details
     * @return Registration response containing applicationId and customerId
     */
    VegapayCustomerRegistrationResponse registerCustomer(VegapayCustomerRegistrationRequest request);

    /**
     * GET /vegapay/api/status
     * Retrieves the workflow state and status for the given application ID.
     *
     * @param applicationId The application ID
     * @return Status response containing applicationId, vegapayWorkflowState, and
     *         vegapayWorkflowStateStatus
     */
    VegapayStatusResponse getStatus(String applicationId);

    VegapayGeneralResponse updatePanDetails(String panNo, String applicationId, String dob);

    VegapayGeneralResponse getEkycUrl(String applicationId);

    VegapayGeneralResponse updatePermanentAddress(AddressRequest permanentAddressRequest);

    VegapayGeneralResponse updateCurrentAddress(AddressRequest currentAddressRequest);

    VegapayGeneralResponse callForEkycVerification(String applicationId);

    VegapayGeneralResponse generateVkycUrl(String applicationId);

    VegapayGeneralResponse generateLimit(String applicationId, String salary);

    VegapayGeneralResponse getLimit(String applicationId);
}
