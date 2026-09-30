package org.example.dao;

import org.example.model.entites.WorkflowEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

@Service
public class WorkflowMockRepository {

    private final HashMap<String, WorkflowEntity> workflowEntities = new HashMap<>();

    public Optional<WorkflowEntity> getWorkflowEntity(String id) {
        return Optional.ofNullable(workflowEntities.get(id));
    }

    public void save(WorkflowEntity workflowEntity) {
        workflowEntities.put(workflowEntity.getWorkflowId(), workflowEntity);
    }
}
