package org.example.dto.request;

import lombok.Data;
import org.example.dto.WorkflowState;

@Data
public class WorkflowUpdateRequest {
    private String workflowId;
    private WorkflowState workflowState;
    private WorkflowStateRequestPayload workflowStateRequestPayload;
    private String applicationId;
}
