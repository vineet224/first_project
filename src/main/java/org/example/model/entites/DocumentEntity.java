package org.example.model.entites;

import lombok.Data;
import org.example.dto.WorkflowState;
import org.example.model.DocumentDetails;

@Data
public class DocumentEntity {
    private String workflowId;
    private String documentId;
    private WorkflowState workflowState;
    private DocumentDetails documentDetails;
}
