package org.example.dto.request;

import lombok.Data;

@Data
public class EkycDetailRequest implements WorkflowStateRequestPayload {
    private AddressRequest currentAddress;
    private AddressRequest permanentAddress;
}
