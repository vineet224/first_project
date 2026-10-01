package org.example.service;

import java.util.Optional;
import java.util.UUID;

import org.example.dao.DocumentMockRepository;
import org.example.dao.WorkflowMockRepository;
import org.example.dto.WorkflowState;
import org.example.dto.request.EkycDetailRequest;
import org.example.dto.request.PanDetailsRequest;
import org.example.dto.request.VkycDetailRequest;
import org.example.dto.request.WorkflowUpdateRequest;
import org.example.model.EkycDocument;
import org.example.model.PanDocument;
import org.example.model.SalaryDocument;
import org.example.model.VkycDocument;
import org.example.model.entites.DocumentEntity;
import org.example.model.entites.WorkflowEntity;
import org.example.vegapay.VegapayWorkflowState;
import org.example.vegapay.client.VegapayClient;
import org.example.vegapay.response.VegapayGeneralResponse;
import org.example.vegapay.response.VegapayStatusResponse;
import org.example.vegapay.statehandlers.VegapayStateHandler;
import org.springframework.stereotype.Service;

@Service
public class WorkflowStateHandlers {

    WorkflowMockRepository workflowMockRepository;
    DocumentMockRepository documentMockRepository;
    WorkflowTransitionService workflowTransitionService;
    VegapayStateHandler vegapayStateHandler;
    VegapayClient vegapayClient;
    private final VegapayProgressionService vegapayProgressionService;

    public WorkflowStateHandlers(
            WorkflowMockRepository workflowMockRepository,
            DocumentMockRepository documentMockRepository,
            WorkflowTransitionService workflowTransitionService,
            VegapayStateHandler vegapayStateHandler,
            VegapayClient vegapayClient,
            VegapayProgressionService vegapayProgressionService) {
        this.workflowMockRepository = workflowMockRepository;
        this.documentMockRepository = documentMockRepository;
        this.workflowTransitionService = workflowTransitionService;
        this.vegapayStateHandler = vegapayStateHandler;
        this.vegapayClient = vegapayClient;
        this.vegapayProgressionService = vegapayProgressionService;
    }

    public void handlePanValidation(WorkflowUpdateRequest updateRequest, WorkflowEntity workflow) {

        // here we should also get the vegapay get first and check if the state is in
        // correct state from the vegapay

        VegapayStatusResponse vegapayStatusResponse = vegapayClient.getStatus(updateRequest.getApplicationId());

        if (vegapayStatusResponse.getVegapayWorkflowState() == VegapayWorkflowState.Application_Rejected) {
            workflow.setWorkflowState(WorkflowState.REJECTED);
            workflowMockRepository.save(workflow);
            return;
        }

        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setWorkflowId(updateRequest.getWorkflowId());
        documentEntity.setWorkflowState(updateRequest.getWorkflowState());
        String documentId = UUID.randomUUID().toString();
        documentEntity.setDocumentId(documentId);

        PanDetailsRequest panDetailsRequest = (PanDetailsRequest) updateRequest.getWorkflowStateRequestPayload();
        PanDocument panDocumentDetails = new PanDocument();
        panDocumentDetails.setPanNo(panDetailsRequest.getPanNo());
        panDocumentDetails.setDob(panDetailsRequest.getDob());
        panDocumentDetails.setPanName(panDetailsRequest.getPanName());

        documentEntity.setDocumentDetails(panDocumentDetails);

        documentMockRepository.save(documentEntity); // this is update not create

        VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.updatePanDetails(panDetailsRequest.getPanNo(),
                updateRequest.getApplicationId(), panDetailsRequest.getDob());

        if (vegapayGeneralResponse.getPanUpdateStatus() == VegapayGeneralResponse.EPanUpdateStatus.Accepted) {
            WorkflowState nextState = WorkflowTransitionService.transitionTo(updateRequest.getWorkflowState(),
                    WorkflowState.PAN_VALIDATION_PENDING);
            workflow.setWorkflowState(nextState);
            workflowMockRepository.save(workflow);
        } else if (vegapayGeneralResponse.getPanUpdateStatus() == VegapayGeneralResponse.EPanUpdateStatus.Declined) {
            // just return the error to frontend so they can retry
        }

    }

    public void handlePanValidationPending(WorkflowUpdateRequest updateRequest, WorkflowEntity workflow) {
        vegapayProgressionService.pollVegapay(VegapayWorkflowState.Ekyc_Url_Generated,
                workflow.getWorkflowId(),
                workflow.getApplicationId());
    }

    public void handleEkyc(WorkflowUpdateRequest updateRequest, WorkflowEntity workflowEntity) {

        VegapayStatusResponse vegapayStatusResponse = vegapayClient.getStatus(updateRequest.getApplicationId());

        if (vegapayStatusResponse.getVegapayWorkflowState() == VegapayWorkflowState.Application_Rejected) {
            workflowEntity.setWorkflowState(WorkflowState.REJECTED);
            workflowMockRepository.save(workflowEntity);
            return;
        }

        Optional<DocumentEntity> docmeOptional = documentMockRepository
                .findByWorkflowIdAndState(updateRequest.getWorkflowId(), updateRequest.getWorkflowState());
        if (docmeOptional.isEmpty()) {
            throw new RuntimeException("Ekyc Document not found");
        }
        DocumentEntity docme = docmeOptional.get();
        EkycDocument ekycDocumentDetails = (EkycDocument) docme.getDocumentDetails();

        EkycDetailRequest ekycDetailRequest = (EkycDetailRequest) updateRequest.getWorkflowStateRequestPayload();

        ekycDocumentDetails.setCurrentAddress(ekycDetailRequest.getCurrentAddress());
        ekycDocumentDetails.setPermanentAddress(ekycDetailRequest.getPermanentAddress());

        docme.setDocumentDetails(ekycDocumentDetails);
        documentMockRepository.save(docme);

        WorkflowState nextState = WorkflowTransitionService.transitionTo(updateRequest.getWorkflowState(),
                WorkflowState.EKYC_PENDING);
        workflowEntity.setWorkflowState(nextState);
        workflowMockRepository.save(workflowEntity);

        return;

    }

    public void handleEkycPending(WorkflowUpdateRequest updateRequest, WorkflowEntity workflowEntity) {
        vegapayProgressionService.pollVegapay(VegapayWorkflowState.Vkyc,
                workflowEntity.getWorkflowId(),
                workflowEntity.getApplicationId());
    }

    public void handleVkyc(WorkflowUpdateRequest updateRequest, WorkflowEntity workflowEntity) {
        VegapayStatusResponse vegapayStatusResponse = vegapayClient.getStatus(updateRequest.getApplicationId());

        if (vegapayStatusResponse.getVegapayWorkflowState() == VegapayWorkflowState.Application_Rejected) {
            workflowEntity.setWorkflowState(WorkflowState.REJECTED);
            workflowMockRepository.save(workflowEntity);
            return;
        }

        Optional<DocumentEntity> optionaldocumentEntity = documentMockRepository
                .findByWorkflowIdAndState(updateRequest.getWorkflowId(), updateRequest.getWorkflowState());

        if (optionaldocumentEntity.isEmpty()) {
            throw new RuntimeException("Vkyc Document not found");
        }
        DocumentEntity documentEntity = optionaldocumentEntity.get();
        VkycDocument vkycDocument = (VkycDocument) documentEntity.getDocumentDetails();

        VkycDetailRequest vkycDetailRequest = (VkycDetailRequest) updateRequest.getWorkflowStateRequestPayload();

        vkycDocument.setVkyc_token(vkycDetailRequest.getVkyc_session_token());

        documentEntity.setDocumentDetails(vkycDocument);
        documentMockRepository.save(documentEntity);

        WorkflowState nextState = WorkflowTransitionService.transitionTo(updateRequest.getWorkflowState(),
                WorkflowState.VKYC_PENDING);
        workflowEntity.setWorkflowState(nextState);
        workflowMockRepository.save(workflowEntity);
        return;

    }

    public void handleVkycPending(WorkflowUpdateRequest updateRequest, WorkflowEntity workflowEntity) {
        vegapayProgressionService.pollVegapay(VegapayWorkflowState.Limit_Generate,
                workflowEntity.getWorkflowId(),
                workflowEntity.getApplicationId());
    }

    public void handleLimitCreation(WorkflowUpdateRequest updateRequest, WorkflowEntity workflowEntity) {
        VegapayStatusResponse vegapayStatusResponse = vegapayClient.getStatus(updateRequest.getApplicationId());

        if (vegapayStatusResponse.getVegapayWorkflowState() == VegapayWorkflowState.Application_Rejected) {
            workflowEntity.setWorkflowState(WorkflowState.REJECTED);
            workflowMockRepository.save(workflowEntity);
            return;
        }

        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setWorkflowId(updateRequest.getWorkflowId());
        documentEntity.setWorkflowState(updateRequest.getWorkflowState());

        String randomDocumentId = UUID.randomUUID().toString();
        documentEntity.setDocumentId(randomDocumentId);

        SalaryDocument salaryDocument = (SalaryDocument) updateRequest.getWorkflowStateRequestPayload();

        documentEntity.setDocumentDetails(salaryDocument);
        documentMockRepository.save(documentEntity);

        vegapayStateHandler.handleLimit_Generate(updateRequest.getApplicationId(), updateRequest.getWorkflowId());

        WorkflowState nextState = WorkflowTransitionService.transitionTo(updateRequest.getWorkflowState(),
                WorkflowState.LIMIT_CREATION_PENDING);
        workflowEntity.setWorkflowState(nextState);
        workflowMockRepository.save(workflowEntity);
        return;

    }

    public void handleLimitCreationPending(WorkflowUpdateRequest updateRequest, WorkflowEntity workflowEntity) {
        vegapayProgressionService.pollVegapay(VegapayWorkflowState.Offer_Generated,
                workflowEntity.getWorkflowId(),
                workflowEntity.getApplicationId());
    }

}
