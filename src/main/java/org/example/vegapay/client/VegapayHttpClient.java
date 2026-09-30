package org.example.vegapay.client;

import org.example.dto.request.AddressRequest;
import org.example.vegapay.request.VegapayCustomerRegistrationRequest;
import org.example.vegapay.response.VegapayCustomerRegistrationResponse;
import org.example.vegapay.response.VegapayGeneralResponse;
import org.example.vegapay.response.VegapayStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP Client for Vegapay APIs.
 * Supports calling real Vegapay endpoints via HTTP, or returning mock responses
 * directly via MockVegapayService when mock mode is enabled (default: true).
 */
@Component
public class VegapayHttpClient implements VegapayClient {

    private final String baseUrl;
    private boolean mockEnabled;
    private final MockVegapayService mockVegapayService;
    private final RestClient restClient;

    public VegapayHttpClient() {
        this("http://localhost:8080", true, new MockVegapayService(), RestClient.builder());
    }

    public VegapayHttpClient(String baseUrl) {
        this(baseUrl, false, new MockVegapayService(), RestClient.builder());
    }

    public VegapayHttpClient(String baseUrl, boolean mockEnabled) {
        this(baseUrl, mockEnabled, new MockVegapayService(), RestClient.builder());
    }

    @Autowired
    public VegapayHttpClient(
            @Value("${vegapay.client.base-url:http://localhost:8080}") String baseUrl,
            @Value("${vegapay.client.mock:true}") boolean mockEnabled,
            @Autowired(required = false) MockVegapayService mockVegapayService,
            @Autowired(required = false) RestClient.Builder restClientBuilder
    ) {
        this.baseUrl = (baseUrl != null && baseUrl.endsWith("/")) ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.mockEnabled = mockEnabled;
        this.mockVegapayService = mockVegapayService != null ? mockVegapayService : new MockVegapayService();

        RestClient.Builder builder = restClientBuilder != null ? restClientBuilder : RestClient.builder();
        this.restClient = builder.baseUrl(this.baseUrl != null ? this.baseUrl : "http://localhost:8080").build();
    }

    @Override
    public VegapayCustomerRegistrationResponse registerCustomer(VegapayCustomerRegistrationRequest request) {
        if (mockEnabled) {
            return mockVegapayService.registerCustomer(request);
        }

        return restClient.post()
                .uri("/vegapay/api/customer-registration")
                .body(request)
                .retrieve()
                .body(VegapayCustomerRegistrationResponse.class);
    }

    @Override
    public VegapayStatusResponse getStatus(String applicationId) {
        if (mockEnabled) {
            return mockVegapayService.getStatus(applicationId);
        }

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/vegapay/api/status")
                        .queryParam("applicationId", applicationId)
                        .build())
                .retrieve()
                .body(VegapayStatusResponse.class);
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public MockVegapayService getMockVegapayService() {
        return mockVegapayService;
    }
}
