package org.example.vegapay.client;

import org.example.vegapay.VegapayWorkflowState;
import org.example.vegapay.VegapayWorkflowStateStatus;
import org.example.dto.request.AddressRequest;
import org.example.vegapay.request.VegapayCustomerRegistrationRequest;
import org.example.vegapay.response.VegapayCustomerRegistrationResponse;
import org.example.vegapay.response.VegapayGeneralResponse;
import org.example.vegapay.response.VegapayStatusResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MockVegapayService {

    private final Map<String, MockApplicationRecord> recordsByApplicationId = new ConcurrentHashMap<>();

    public record MockApplicationRecord(
            String applicationId,
            String customerId,
            VegapayCustomerRegistrationRequest request,
            VegapayWorkflowState state,
            VegapayWorkflowStateStatus status
    ) {}

    public VegapayCustomerRegistrationResponse registerCustomer(VegapayCustomerRegistrationRequest request) {
        String applicationId = "APP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String customerId = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MockApplicationRecord record = new MockApplicationRecord(
                applicationId,
                customerId,
                request,
                VegapayWorkflowState.PAN_VERIFIVATION,
                VegapayWorkflowStateStatus.IN_PROGRESS
        );
        recordsByApplicationId.put(applicationId, record);

        return new VegapayCustomerRegistrationResponse(applicationId, customerId);
    }

    public VegapayStatusResponse getStatus(String applicationId) {
        if (applicationId != null && recordsByApplicationId.containsKey(applicationId)) {
            MockApplicationRecord record = recordsByApplicationId.get(applicationId);
            return new VegapayStatusResponse(record.applicationId(), record.state(), record.status());
        }

        // Return sensible default mock response when application ID is new or not found
        String resolvedAppId = (applicationId != null && !applicationId.isBlank()) ? applicationId : "APP-MOCK-001";
        return new VegapayStatusResponse(
                resolvedAppId,
                VegapayWorkflowState.PAN_VERIFIVATION,
                VegapayWorkflowStateStatus.IN_PROGRESS
        );
    }

    public void updateStatus(String applicationId, VegapayWorkflowState state, VegapayWorkflowStateStatus status) {
        recordsByApplicationId.computeIfPresent(applicationId, (appId, old) ->
                new MockApplicationRecord(appId, old.customerId(), old.request(), state, status)
        );
    }

    public VegapayGeneralResponse updatePanDetails(String panNo, String applicationId, String dob) {
        VegapayGeneralResponse response = new VegapayGeneralResponse();
        response.setApplicationId(applicationId);
        response.setWorkflowState(VegapayWorkflowState.PAN_VERIFIVATION);
        response.setWorkflowStateStatus(VegapayWorkflowStateStatus.PNEDING);
        response.setPanUpdateStatus(VegapayGeneralResponse.EPanUpdateStatus.Accepted);
        updateStatus(applicationId, VegapayWorkflowState.Ekyc_Url_Generated, VegapayWorkflowStateStatus.PNEDING);
        return response;
    }

    public VegapayGeneralResponse getEkycUrl(String applicationId) {
        VegapayGeneralResponse response = new VegapayGeneralResponse();
        response.setApplicationId(applicationId);
        response.setWorkflowState(VegapayWorkflowState.Ekyc_Url_Generated);
        response.setWorkflowStateStatus(VegapayWorkflowStateStatus.PNEDING);
        response.setEkycUrl("https://mock.vegapay.com/ekyc/" + applicationId);
        return response;
    }

    public VegapayGeneralResponse updatePermanentAddress(AddressRequest permanentAddressRequest) {
        VegapayGeneralResponse response = new VegapayGeneralResponse();
        response.setWorkflowState(VegapayWorkflowState.Permanent_Address_Details);
        response.setWorkflowStateStatus(VegapayWorkflowStateStatus.PNEDING);
        return response;
    }

    public VegapayGeneralResponse updateCurrentAddress(AddressRequest currentAddressRequest) {
        VegapayGeneralResponse response = new VegapayGeneralResponse();
        response.setWorkflowState(VegapayWorkflowState.Current_Address_Details);
        response.setWorkflowStateStatus(VegapayWorkflowStateStatus.PNEDING);
        return response;
    }

    public VegapayGeneralResponse callForEkycVerification(String applicationId) {
        VegapayGeneralResponse response = new VegapayGeneralResponse();
        response.setApplicationId(applicationId);
        response.setWorkflowState(VegapayWorkflowState.Ekyc_Verification);
        response.setWorkflowStateStatus(VegapayWorkflowStateStatus.PNEDING);
        return response;
    }

    public VegapayGeneralResponse generateVkycUrl(String applicationId) {
        VegapayGeneralResponse response = new VegapayGeneralResponse();
        response.setApplicationId(applicationId);
        response.setWorkflowState(VegapayWorkflowState.Vkyc);
        response.setWorkflowStateStatus(VegapayWorkflowStateStatus.PNEDING);
        String vkycUrl = "https://mock.vegapay.com/vkyc/" + applicationId;
        response.setEkycUrl(vkycUrl);
        response.setVideoKycSessionUrl(vkycUrl);
        return response;
    }

    public VegapayGeneralResponse generateLimit(String applicationId, String salary) {
        VegapayGeneralResponse response = new VegapayGeneralResponse();
        response.setApplicationId(applicationId);
        response.setWorkflowState(VegapayWorkflowState.Limit_Generate);
        response.setWorkflowStateStatus(VegapayWorkflowStateStatus.PNEDING);
        response.setLimitCreated(salary != null ? salary : "500000");
        return response;
    }

    public VegapayGeneralResponse getLimit(String applicationId) {
        VegapayGeneralResponse response = new VegapayGeneralResponse();
        response.setApplicationId(applicationId);
        response.setWorkflowState(VegapayWorkflowState.Offer_Generated);
        response.setWorkflowStateStatus(VegapayWorkflowStateStatus.PNEDING);
        response.setLimitCreated("500000");
        return response;
    }

    public void clear() {
        recordsByApplicationId.clear();
    }
}
