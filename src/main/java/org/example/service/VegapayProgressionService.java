package org.example.service;

import org.example.dao.WorkflowMockRepository;
import org.example.dto.WorkflowState;
import org.example.model.entites.WorkflowEntity;
import org.example.vegapay.VegapayWorkflowState;
import org.example.vegapay.VegapayWorkflowStateStatus;
import org.example.vegapay.client.VegapayClient;
import org.example.vegapay.response.VegapayStatusResponse;
import org.example.vegapay.statehandlers.VegapayStateHandler;
import org.springframework.stereotype.Service;

@Service
public class VegapayProgressionService {
    private final WorkflowMockRepository workflowRepository;
    private final VegapayClient vegapayClient;
    private final VegapayStateHandler stateHandler;

    public VegapayProgressionService(WorkflowMockRepository workflowRepository,
            VegapayClient vegapayClient, VegapayStateHandler stateHandler) {
        this.workflowRepository = workflowRepository;
        this.vegapayClient = vegapayClient;
        this.stateHandler = stateHandler;
    }

    public boolean isPending(WorkflowState state) {
        return state == WorkflowState.PAN_VALIDATION_PENDING
                || state == WorkflowState.EKYC_PENDING
                || state == WorkflowState.VKYC_PENDING
                || state == WorkflowState.LIMIT_CREATION_PENDING;
    }

    public VegapayWorkflowState stopStateFor(WorkflowState state) {
        return switch (state) {
            case PAN_VALIDATION_PENDING -> VegapayWorkflowState.Ekyc_Url_Generated;
            case EKYC_PENDING -> VegapayWorkflowState.Vkyc;
            case VKYC_PENDING -> VegapayWorkflowState.Limit_Generate;
            case LIMIT_CREATION_PENDING -> VegapayWorkflowState.Offer_Generated;
            default -> throw new IllegalArgumentException("Expected a pending workflow state");
        };
    }

    /** Performs one status read and at most one action, stopping at the next input boundary. */
    public boolean pollVegapay(VegapayWorkflowState stopState, String workflowId, String applicationId) {
        WorkflowEntity workflow = workflowRepository.getWorkflowEntity(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));
        WorkflowState pendingState = workflow.getWorkflowState();
        if (stopState != stopStateFor(pendingState)) {
            throw new IllegalArgumentException("Stop state does not match the local pending stage");
        }
        VegapayStatusResponse status = vegapayClient.getStatus(applicationId);
        VegapayWorkflowState remoteState = status.getVegapayWorkflowState();
        if (remoteState == VegapayWorkflowState.Application_Rejected) {
            workflow.setWorkflowState(WorkflowState.REJECTED);
            workflowRepository.save(workflow);
            return true;
        }
        if (status.getVegapayWorkflowStateStatus() != VegapayWorkflowStateStatus.PNEDING) {
            return false;
        }
        if (remoteState == stopState) {
            WorkflowState nextState = switch (stopState) {
                case Ekyc_Url_Generated -> {
                    stateHandler.handleEkyc_Url_Generated(applicationId, workflowId);
                    yield WorkflowState.EKYC;
                }
                case Vkyc -> {
                    stateHandler.handleVkyc(applicationId, workflowId);
                    yield WorkflowState.VKYC;
                }
                case Limit_Generate -> WorkflowState.LIMIT_CREATION;
                case Offer_Generated -> {
                    stateHandler.handleOffer_Generated(applicationId, workflowId);
                    yield WorkflowState.OFFER_GENERATED;
                }
                default -> throw new IllegalArgumentException("Unsupported stop state");
            };
            workflow.setWorkflowState(WorkflowTransitionService.transitionTo(pendingState, nextState));
            workflowRepository.save(workflow);
            return true;
        }
        if (pendingState == WorkflowState.EKYC_PENDING) {
            switch (remoteState) {
                case Permanent_Address_Details -> stateHandler.handlePermanentAddress(applicationId, workflowId);
                case Current_Address_Details -> stateHandler.handleCurrent_address(applicationId, workflowId);
                case Ekyc_Verification -> stateHandler.handleEkyc_Verification(applicationId, workflowId);
                default -> { }
            }
        }
        return false;
    }
}
