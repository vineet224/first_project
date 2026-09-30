package org.example.dto.request;

import lombok.Data;

@Data
public class LimitCreationDetailRequest implements WorkflowStateRequestPayload {
    private String salary;
}
