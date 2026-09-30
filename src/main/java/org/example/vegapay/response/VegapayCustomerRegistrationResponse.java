package org.example.vegapay.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VegapayCustomerRegistrationResponse {

    @JsonProperty("applicationId")
    private String applicationId;

    @JsonProperty("customerId")
    private String customerId;

    public VegapayCustomerRegistrationResponse() {
    }

    public VegapayCustomerRegistrationResponse(String applicationId, String customerId) {
        this.applicationId = applicationId;
        this.customerId = customerId;
    }

    @Override
    public String toString() {
        return "VegapayCustomerRegistrationResponse{" +
                "applicationId='" + applicationId + '\'' +
                ", customerId='" + customerId + '\'' +
                '}';
    }
}
