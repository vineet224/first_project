package org.example.vegapay.client;

import lombok.Getter;
import org.example.dto.request.AddressRequest;
import org.example.vegapay.request.VegapayCustomerRegistrationRequest;
import org.example.vegapay.response.VegapayCustomerRegistrationResponse;
import org.example.vegapay.response.VegapayGeneralResponse;
import org.example.vegapay.response.VegapayStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * In-memory Mock implementation of VegapayClient.
 * Returns mock responses without requiring an external Vegapay service.
 */
@Getter
@Component
@Primary
public class MockVegapayClient implements VegapayClient {

    private final MockVegapayService mockVegapayService;

    @Autowired
    public MockVegapayClient(MockVegapayService mockVegapayService) {
        this.mockVegapayService = mockVegapayService;
    }

    public MockVegapayClient() {
        this(new MockVegapayService());
    }

    @Override
    public VegapayCustomerRegistrationResponse registerCustomer(VegapayCustomerRegistrationRequest request) {
        return mockVegapayService.registerCustomer(request);
    }

    @Override
    public VegapayStatusResponse getStatus(String applicationId) {
        return mockVegapayService.getStatus(applicationId);
    }

    @Override
    public VegapayGeneralResponse updatePanDetails(String panNo, String applicationId, String dob) {
        return mockVegapayService.updatePanDetails(panNo, applicationId, dob);
    }

    @Override
    public VegapayGeneralResponse getEkycUrl(String applicationId) {
        return mockVegapayService.getEkycUrl(applicationId);
    }

    @Override
    public VegapayGeneralResponse updatePermanentAddress(AddressRequest permanentAddressRequest) {
        return mockVegapayService.updatePermanentAddress(permanentAddressRequest);
    }

    @Override
    public VegapayGeneralResponse updateCurrentAddress(AddressRequest currentAddressRequest) {
        return mockVegapayService.updateCurrentAddress(currentAddressRequest);
    }

    @Override
    public VegapayGeneralResponse callForEkycVerification(String applicationId) {
        return mockVegapayService.callForEkycVerification(applicationId);
    }

    @Override
    public VegapayGeneralResponse generateVkycUrl(String applicationId) {
        return mockVegapayService.generateVkycUrl(applicationId);
    }

    @Override
    public VegapayGeneralResponse generateLimit(String applicationId, String salary) {
        return mockVegapayService.generateLimit(applicationId, salary);
    }

    @Override
    public VegapayGeneralResponse getLimit(String applicationId) {
        return mockVegapayService.getLimit(applicationId);
    }

}
