package org.example.exception;

import org.example.dto.WorkflowState;

import java.util.Set;

public class InvalidWorkflowTransitionException extends IllegalStateException {

    private final WorkflowState previousState;
    private final WorkflowState nextState;

    public InvalidWorkflowTransitionException(String message) {
        super(message);
        this.previousState = null;
        this.nextState = null;
    }

    public InvalidWorkflowTransitionException(WorkflowState previousState, WorkflowState nextState, Set<WorkflowState> allowedStates) {
        super(String.format("Invalid workflow transition from '%s' to '%s'. Allowed transition(s): %s",
                previousState,
                nextState,
                (allowedStates == null || allowedStates.isEmpty()) ? "None (terminal state)" : allowedStates));
        this.previousState = previousState;
        this.nextState = nextState;
    }

    public WorkflowState getPreviousState() {
        return previousState;
    }

    public WorkflowState getNextState() {
        return nextState;
    }
}
