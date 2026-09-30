package org.example.service;

import org.example.dao.DocumentMockRepository;
import org.example.dao.UserWorkflowMappingRepository;
import org.example.dao.WorkflowMockRepository;
import org.example.dto.WorkflowState;
import org.example.dto.request.WorkflowCreationRequest;
import org.example.dto.request.WorkflowUpdateRequest;
import org.example.dto.response.WorkflowGetOrUpdateSingleFrontendResponse;
import org.example.model.entites.UserWorkflowMappingEntity;
import org.example.model.entites.WorkflowEntity;
import org.example.vegapay.client.VegapayClient;
import org.example.vegapay.request.VegapayCustomerRegistrationRequest;
import org.example.vegapay.response.VegapayCustomerRegistrationResponse;

import java.time.Duration;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class WorkflowOrchestrator {

    private final WorkflowResponseBuilder workflowResponseBuilder;
    private WorkflowMockRepository workflowMockRepository;
    private DocumentMockRepository documentMockRepository;
    private UserWorkflowMappingRepository userWorkflowMappingRepository;
    private VegapayClient vegapayClient;

    public WorkflowOrchestrator(WorkflowResponseBuilder workflowResponseBuilder,
            UserWorkflowMappingRepository userWorkflowMappingRepository) {
        this.workflowResponseBuilder = workflowResponseBuilder;
        this.userWorkflowMappingRepository = userWorkflowMappingRepository;
        this.workflowMockRepository = new WorkflowMockRepository();
        this.documentMockRepository = new DocumentMockRepository();
    }

    public WorkflowGetOrUpdateSingleFrontendResponse getWorkflowFrontendResponse(String workflowId) {
        WorkflowGetOrUpdateSingleFrontendResponse response = new WorkflowGetOrUpdateSingleFrontendResponse();
        return workflowResponseBuilder.buildWorkflowGetOrUpdateSingleFrontendResponse(response);
    }

    public WorkflowGetOrUpdateSingleFrontendResponse createWorkflow(WorkflowCreationRequest workflowCreationRequest) {
        Optional<UserWorkflowMappingEntity> optionalUserWorkflowMappingEntity=userWorkflowMappingRepository.findByMobileNo(workflowCreationRequest.getMobileNo());
        if(optionalUserWorkflowMappingEntity.isEmpty()){
            return handleWorkflowCreation(workflowCreationRequest);
        }


        UserWorkflowMappingEntity userWorkflowMappingEntity=optionalUserWorkflowMappingEntity.get();

        if(userWorkflowMappingEntity.getStatus()==UserWorkflowMappingEntity.Status.COMPLETED){
            return workflowResponseBuilder.buildWorkflowGetOrUpdateSingleFrontendResponse(new WorkflowGetOrUpdateSingleFrontendResponse());
        }

        if (userWorkflowMappingEntity.getStatus() == UserWorkflowMappingEntity.Status.DISCARDED) {
            if (userWorkflowMappingEntity.getCreatedAt() != null
                    && Duration.between(userWorkflowMappingEntity.getCreatedAt(), LocalDateTime.now()).toDays() >= 30) {
                return handleWorkflowCreation(workflowCreationRequest);
            } else {
                throw new RuntimeException("Workflow Discarded is less than 30 days please come after few days");
            }
        }

        if (userWorkflowMappingEntity.getStatus() == UserWorkflowMappingEntity.Status.PENDING) {
            return workflowResponseBuilder.buildWorkflowGetOrUpdateSingleFrontendResponse(new WorkflowGetOrUpdateSingleFrontendResponse());
        }

        throw new IllegalStateException("Unhandled workflow status: " + userWorkflowMappingEntity.getStatus());
    }

    private WorkflowGetOrUpdateSingleFrontendResponse handleWorkflowCreation(
            WorkflowCreationRequest workflowCreationRequest) {

        VegapayCustomerRegistrationRequest vegapayCustomerRegistrationRequest = new VegapayCustomerRegistrationRequest();

        vegapayCustomerRegistrationRequest.setPhoneNo(workflowCreationRequest.getMobileNo());
        vegapayCustomerRegistrationRequest.setCustomername(workflowCreationRequest.getName());

        try {
            VegapayCustomerRegistrationResponse vegapayCustomerRegistrationResponse = vegapayClient
                    .registerCustomer(vegapayCustomerRegistrationRequest);
            String applicationId = vegapayCustomerRegistrationResponse.getApplicationId();
            String customerId = vegapayCustomerRegistrationResponse.getCustomerId();
            String randomWorkflowId = UUID.randomUUID().toString();

            UserWorkflowMappingEntity userWorkflowMappingEntity = new UserWorkflowMappingEntity();
            userWorkflowMappingEntity.setMobileNo(workflowCreationRequest.getMobileNo());
            userWorkflowMappingEntity.setWorkflowId(randomWorkflowId);
            userWorkflowMappingEntity.setApplicationId(applicationId);
            userWorkflowMappingEntity.setStatus(UserWorkflowMappingEntity.Status.PENDING);
            userWorkflowMappingEntity.setCreatedAt(LocalDateTime.now());
            userWorkflowMappingEntity.setUpdatedAt(LocalDateTime.now());
            userWorkflowMappingRepository.save(userWorkflowMappingEntity);

            WorkflowEntity workflowEntity = new WorkflowEntity();
            workflowEntity.setWorkflowId(randomWorkflowId);
            workflowEntity.setApplicationId(applicationId);
            workflowEntity.setWorkflowState(WorkflowState.PAN_VALIDATION);
            workflowMockRepository.save(workflowEntity);

            WorkflowGetOrUpdateSingleFrontendResponse workflowGetOrUpdateSingleFrontendResponse = new WorkflowGetOrUpdateSingleFrontendResponse();
            workflowGetOrUpdateSingleFrontendResponse.setWorkflowId(randomWorkflowId);
            workflowGetOrUpdateSingleFrontendResponse.setWorkflowState(WorkflowState.PAN_VALIDATION);

            return workflowResponseBuilder
                    .buildWorkflowGetOrUpdateSingleFrontendResponse(workflowGetOrUpdateSingleFrontendResponse);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create workflow");
        }

    }

    public WorkflowGetOrUpdateSingleFrontendResponse updateWorkflow(WorkflowUpdateRequest workflowUpdateRequest) {
        Optional<WorkflowEntity> optionalworkflow = workflowMockRepository
                .getWorkflowEntity(workflowUpdateRequest.getWorkflowId());
        if (optionalworkflow.isEmpty()) {
            System.out.printf("Workflow Not Found");
            throw new IllegalArgumentException("Workflow Not Found");
        }

        WorkflowEntity workflowEntity = optionalworkflow.get();

        if (workflowEntity.getWorkflowState() != workflowUpdateRequest.getWorkflowState()) {
            System.out.println("Workflow State Mismatch");
            throw new IllegalArgumentException("Workflow State Mismatch");
        }

        if (workflowEntity.getWorkflowState() == WorkflowState.OFFER_GENERATED) {
            // this is the terminal state so update request comes here is bad
            System.out.println("Workflow State Offer Generated");
            throw new IllegalArgumentException("Workflow State Offer Generated");
        }

        // now if it is a pending state so this must be pollig call from the frontend
        // now check until the expected state "ABC" from the vegapay and until keep
        // polling and making vegapay api call to update their state and
        // after all that update the state according to the "ABC" at the db workflow
        // state and the ekyc document of vkyc document or any other is alredy updated
        // by the vegapay state handler
        WorkflowGetOrUpdateSingleFrontendResponse response = new WorkflowGetOrUpdateSingleFrontendResponse();
        return response;
    }
}
