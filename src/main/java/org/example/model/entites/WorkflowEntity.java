package org.example.model.entites;

import lombok.Data;
import org.example.dto.WorkflowState;

@Data
public class WorkflowEntity {
    private String workflowId;
    private String userid;
    private String applicationId;
    private WorkflowState workflowState;
}
