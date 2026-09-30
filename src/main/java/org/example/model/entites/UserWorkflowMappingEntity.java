package org.example.model.entites;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserWorkflowMappingEntity {

    public enum Status{
        COMPLETED,
        DISCARDED,
        PENDING
    }

    String workflowId;
    String mobileNo;
    String applicationId;
    LocalDateTime updatedAt;
    LocalDateTime createdAt;
    Status status;

}
