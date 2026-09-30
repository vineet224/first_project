package org.example.service;

import org.example.dao.DocumentMockRepository;
import org.example.dto.WorkflowState;
import org.example.dto.response.WorkflowGetOrUpdateSingleFrontendResponse;
import org.example.model.EkycDocument;
import org.example.model.OfferDocument;
import org.example.model.VkycDocument;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WorkflowResponseBuilder {

    private final DocumentMockRepository documentMockRepository;

    WorkflowResponseBuilder(DocumentMockRepository documentMockRepository) {
        this.documentMockRepository = documentMockRepository;
    }

    WorkflowGetOrUpdateSingleFrontendResponse buildWorkflowGetOrUpdateSingleFrontendResponse(WorkflowGetOrUpdateSingleFrontendResponse response) {
        WorkflowState workflowState = response.getWorkflowState();


        if(workflowState.equals(WorkflowState.EKYC)){
            Optional<EkycDocument> ekycDocument = documentMockRepository.findDocumentDetailsByWorkflowIdAndState(response.getWorkflowId(),workflowState,EkycDocument.class);
            if(ekycDocument.isPresent()){
                System.out.println("Ekyc document found");
                response.setEkyc_url(ekycDocument.get().getEkyc_url());
            }
        }

        if(workflowState.equals(WorkflowState.VKYC)){
            Optional<VkycDocument> vkycDocument = documentMockRepository.findDocumentDetailsByWorkflowIdAndState(response.getWorkflowId(),workflowState,VkycDocument.class);
            if(vkycDocument.isPresent()){
                System.out.println("Vkyc document found");
                response.setVkyc_url(vkycDocument.get().getVkyc_url());
            }
        }

        if(workflowState.equals(WorkflowState.OFFER_GENERATED)){
            Optional<OfferDocument> offerDocument = documentMockRepository.findDocumentDetailsByWorkflowIdAndState(response.getWorkflowId(),workflowState,OfferDocument.class);
            if(offerDocument.isPresent()){
                System.out.println("Offer document found");
                response.setLimit_alloted(offerDocument.get().getLimit());
            }
        }

        return response;
    }
}
