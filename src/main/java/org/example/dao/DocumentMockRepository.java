package org.example.dao;

import org.example.dto.WorkflowState;
import org.example.model.DocumentDetails;
import org.example.model.entites.DocumentEntity;
import org.example.model.entites.WorkflowEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentMockRepository {


    // Primary store: documentId -> DocumentEntity
    private final Map<String, DocumentEntity> documentsById = new ConcurrentHashMap<>();

    // Index: workflowId -> (workflowState -> DocumentEntity)
    private final Map<String, Map<WorkflowState, DocumentEntity>> workflowStateIndex = new ConcurrentHashMap<>();

    public void save(DocumentEntity entity) {
        documentsById.put(entity.getDocumentId(), entity);
        workflowStateIndex
                .computeIfAbsent(entity.getWorkflowId(), k -> new ConcurrentHashMap<>())
                .put(entity.getWorkflowState(), entity);
    }

    public Optional<DocumentEntity> findByWorkflowIdAndState(String workflowId, WorkflowState state) {
        Map<WorkflowState, DocumentEntity> stateMap = workflowStateIndex.get(workflowId);
        if (stateMap == null) return Optional.empty();
        return Optional.ofNullable(stateMap.get(state));
    }

    public Optional<String> findDocumentIdByWorkflowIdAndState(String workflowId, WorkflowState state) {
        return findByWorkflowIdAndState(workflowId, state)
                .map(DocumentEntity::getDocumentId);
    }

    public Optional<DocumentDetails> findDocumentDetailsByWorkflowIdAndState(String workflowId, WorkflowState state) {
        return findByWorkflowIdAndState(workflowId, state)
                .map(DocumentEntity::getDocumentDetails);
    }

    public <T extends DocumentDetails> Optional<T> findDocumentDetailsByWorkflowIdAndState(
            String workflowId,
            WorkflowState state,
            Class<T> expectedType) {

        return findByWorkflowIdAndState(workflowId, state)
                .map(DocumentEntity::getDocumentDetails)
                .filter(expectedType::isInstance)
                .map(expectedType::cast);
    }

}
