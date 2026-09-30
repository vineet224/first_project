package org.example.dto;

import org.example.service.WorkflowTransitionService;

import java.util.Set;

public enum WorkflowState {
    PAN_VALIDATION,
    PAN_VALIDATION_PENDING,
    EKYC,
    EKYC_PENDING,
    VKYC,
    VKYC_PENDING,
    LIMIT_CREATION,
    LIMIT_CREATION_PENDING,
    REJECTED,
    OFFER_GENERATED;

    /**
     * Transition from this state to the next state.
     * Throws InvalidWorkflowTransitionException if transition is invalid.
     */
    public WorkflowState transitionTo(WorkflowState nextState) {
        return WorkflowTransitionService.transitionTo(this, nextState);
    }

    /**
     * Transition from previousState to nextState.
     * Throws InvalidWorkflowTransitionException if transition is invalid.
     */
    public static WorkflowState transitionTo(WorkflowState previousState, WorkflowState nextState) {
        return WorkflowTransitionService.transitionTo(previousState, nextState);
    }

    /**
     * Alias matching exact user prompt method naming: TransitionTO(previousState,
     * NextState)
     */
    public static WorkflowState TransitionTO(WorkflowState previousState, WorkflowState nextState) {
        return WorkflowTransitionService.transitionTo(previousState, nextState);
    }

    /**
     * Check if transition to nextState is valid from this state.
     */
    public boolean canTransitionTo(WorkflowState nextState) {
        return WorkflowTransitionService.isValidTransition(this, nextState);
    }

    /**
     * Get valid next states from this state.
     */
    public Set<WorkflowState> getAllowedNextStates() {
        return WorkflowTransitionService.getAllowedNextStates(this);
    }
}
