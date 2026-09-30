package org.example.dto.request;

import lombok.Data;

@Data
public class PanDetailsRequest implements WorkflowStateRequestPayload {
    private String panName;
    private String dob;
    private String panNo;
}
