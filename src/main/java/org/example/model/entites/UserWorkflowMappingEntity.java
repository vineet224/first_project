package org.example.model.entites;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserWorkflowMappingEntity {

    public static enum Status {
        PENDING,
        COMPLETED,
        DISCARDED,
    }

    String mobileNo;
    String workflowId;
    String applicationId;
    Status status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
