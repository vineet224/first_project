package org.example.model;

import lombok.Data;
import org.example.dto.WorkflowState;
import org.example.dto.request.AddressRequest;

@Data
public class EkycDocument implements DocumentDetails {
    private AddressRequest permanentAddress;
    private AddressRequest currentAddress;
    private String ekyc_url;
}
