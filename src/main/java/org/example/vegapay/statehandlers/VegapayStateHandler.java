package org.example.vegapay.statehandlers;

import org.example.dao.DocumentMockRepository;
import org.example.dao.WorkflowMockRepository;
import org.example.dto.WorkflowState;
import org.example.dto.request.AddressRequest;
import org.example.model.DocumentDetails;
import org.example.model.EkycDocument;
import org.example.model.OfferDocument;
import org.example.model.PanDocument;
import org.example.model.VkycDocument;
import org.example.model.SalaryDocument;
import org.example.model.entites.DocumentEntity;
import org.example.vegapay.VegapayWorkflowState;
import org.example.vegapay.VegapayWorkflowStateStatus;
import org.example.vegapay.client.VegapayClient;
import org.example.vegapay.response.VegapayGeneralResponse;
import org.example.vegapay.response.VegapayStatusResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
public class VegapayStateHandler {
    private final WorkflowMockRepository workflowMockRepository;
    private final DocumentMockRepository documentMockRepository;
    private final VegapayClient vegapayClient;

    public VegapayStateHandler(WorkflowMockRepository workflowMockRepository,
            DocumentMockRepository documentMockRepository, VegapayClient vegapayClient) {
        this.workflowMockRepository = workflowMockRepository;
        this.documentMockRepository = documentMockRepository;
        this.vegapayClient = vegapayClient;
    }

    public void handleVegapayState(String applicationId, String workflowId) {
        VegapayStatusResponse statusResponse = vegapayClient.getStatus(applicationId);

        if (statusResponse.getVegapayWorkflowStateStatus() != VegapayWorkflowStateStatus.PNEDING) {
            System.out.printf("The vegapay state is not in pending means don't require action from our end ");
            return;
        }

        switch (statusResponse.getVegapayWorkflowState()) {
            case PAN_VERIFIVATION:
                handlePanValidationState(applicationId, workflowId);
                break;
            case Ekyc_Url_Generated:
                handleEkyc_Url_Generated(applicationId, workflowId);
                break;
            case Current_Address_Details:
                handleCurrent_address(applicationId, workflowId);
                break;
            case Permanent_Address_Details:
                handlePermanentAddress(applicationId, workflowId);
                break;
            case Ekyc_Verification:
                handleEkyc_Verification(applicationId, workflowId);
                break;
            case RE_Ekyc_Verification:
                handleREkyc_verification(applicationId, workflowId);
                break;
            case Vkyc:
                handleVkyc(applicationId, workflowId);
                break;
            case Limit_Generate:
                handleLimit_Generate(applicationId, workflowId);
                break;
            case Offer_Generated:
                handleOffer_Generated(applicationId, workflowId);
                break;
            case Application_Rejected:
                break;
            default:
                break;
        }

    }

    public void handlePanValidationState(String applicationId, String workflowId) {
        Optional<PanDocument> panDocumentOptional = documentMockRepository
                .findDocumentDetailsByWorkflowIdAndState(workflowId, WorkflowState.PAN_VALIDATION, PanDocument.class);
        if (panDocumentOptional.isPresent()) {
            PanDocument panDocument = panDocumentOptional.get();
            vegapayClient.updatePanDetails(panDocument.getPanNo(), applicationId, panDocument.getDob());
        }
    }

    public void handleEkyc_Url_Generated(String applicationId, String workflowId) {
        EkycDocument ekycDocument = new EkycDocument();
        VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.getEkycUrl(applicationId);
        ekycDocument.setEkyc_url(vegapayGeneralResponse.getEkycUrl());
        Random uuidGenerator = new Random();
        String documentId = String.valueOf(uuidGenerator.nextInt());
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentId(documentId);
        documentEntity.setWorkflowId(workflowId);
        documentEntity.setWorkflowState(WorkflowState.EKYC);
        documentEntity.setDocumentDetails(ekycDocument);
        documentMockRepository.save(documentEntity);
    }

    public VegapayGeneralResponse handleCurrent_address(String applicationId, String workflowId) {
        Optional<EkycDocument> optionEkycDocument = documentMockRepository
                .findDocumentDetailsByWorkflowIdAndState(workflowId, WorkflowState.EKYC, EkycDocument.class);
        if (optionEkycDocument.isPresent()) {
            EkycDocument ekycDocument = optionEkycDocument.get();
            AddressRequest addressRequest = ekycDocument.getCurrentAddress();
            VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.updateCurrentAddress(addressRequest);
            return vegapayGeneralResponse;
        }
        throw new IllegalArgumentException("Ekyc document not found for workflow id: " + workflowId);
    }

    public VegapayGeneralResponse handlePermanentAddress(String applicationId, String workflowId) {
        Optional<EkycDocument> optionEkycDocument = documentMockRepository
                .findDocumentDetailsByWorkflowIdAndState(workflowId, WorkflowState.EKYC, EkycDocument.class);
        if (optionEkycDocument.isPresent()) {
            EkycDocument ekycDocument = optionEkycDocument.get();
            AddressRequest addressRequest = ekycDocument.getPermanentAddress();
            VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.updatePermanentAddress(addressRequest);
            return vegapayGeneralResponse;
        }
        throw new IllegalArgumentException("Ekyc document not found for workflow id: " + workflowId);
    }

    public VegapayGeneralResponse handleEkyc_Verification(String applicationId, String workflowId) {
        VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.callForEkycVerification(applicationId);
        return vegapayGeneralResponse;
    }

    public VegapayGeneralResponse handleREkyc_verification(String applicationId, String workflowId) {
        VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.callForEkycVerification(applicationId);
        return vegapayGeneralResponse;
    }

    public VegapayGeneralResponse handleVkyc(String applicationId, String workflowId) {
        VkycDocument vkycDocument = new VkycDocument();
        VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.generateVkycUrl(applicationId);

        vkycDocument.setVkyc_url(vegapayGeneralResponse.getEkycUrl());
        Random uuidGenerator = new Random();
        String documentId = String.valueOf(uuidGenerator.nextInt());
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentId(documentId);
        documentEntity.setWorkflowId(workflowId);
        documentEntity.setWorkflowState(WorkflowState.VKYC);
        documentEntity.setDocumentDetails(vkycDocument);
        documentMockRepository.save(documentEntity);

        return vegapayGeneralResponse;
    }

    public VegapayGeneralResponse handleLimit_Generate(String applicationId, String workflowId) {
        Optional<SalaryDocument> salaryDocumentOptional = documentMockRepository
                .findDocumentDetailsByWorkflowIdAndState(workflowId, WorkflowState.LIMIT_CREATION,
                        SalaryDocument.class);
        if (salaryDocumentOptional.isPresent()) {
            SalaryDocument salaryDocument = salaryDocumentOptional.get();
            VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.generateLimit(applicationId,
                    salaryDocument.getSalary());

            return vegapayGeneralResponse;
        }
        throw new IllegalArgumentException("Salary document not found for workflow id: " + workflowId);
    }

    public VegapayGeneralResponse handleOffer_Generated(String applicationId, String workflowId) {

        OfferDocument offerDocument = new OfferDocument();
        VegapayGeneralResponse vegapayGeneralResponse = vegapayClient.getLimit(applicationId);
        offerDocument.setLimit(vegapayGeneralResponse.getLimitCreated());

        Random uuidGenerator = new Random();
        String documentId = String.valueOf(uuidGenerator.nextInt());
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentId(documentId);
        documentEntity.setWorkflowId(workflowId);
        documentEntity.setWorkflowState(WorkflowState.OFFER_GENERATED);
        documentEntity.setDocumentDetails(offerDocument);
        documentMockRepository.save(documentEntity);

        return vegapayGeneralResponse;
    }

}
