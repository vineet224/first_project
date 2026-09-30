package org.example.vegapay.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class VegapayCustomerRegistrationRequest {

    @JsonProperty("customername")
    @JsonAlias({ "customerName", "customer_name" })
    private String customername;

    @JsonProperty("phoneNo")
    @JsonAlias({ "phoneno", "phone_no", "phoneNumber" })
    private String phoneNo;

    public VegapayCustomerRegistrationRequest() {
    }

    public VegapayCustomerRegistrationRequest(String customername, String customerdob, String customerpan,
            String phoneNo) {
        this.customername = customername;
        this.phoneNo = phoneNo;
    }

    @Override
    public String toString() {
        return "VegapayCustomerRegistrationRequest{" +
                "customername='" + customername + '\'' +
                ", phoneNo='" + phoneNo + '\'' +
                '}';
    }
}
