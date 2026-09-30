package org.example.service;

import org.example.dto.WorkflowState;
import org.example.exception.InvalidWorkflowTransitionException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Service
public class WorkflowTransitionService {

    private static final Map<WorkflowState, Set<WorkflowState>> ALLOWED_TRANSITIONS = new EnumMap<>(
            WorkflowState.class);

    static {
        ALLOWED_TRANSITIONS.put(WorkflowState.PAN_VALIDATION, Set.of(WorkflowState.PAN_VALIDATION_PENDING));
        ALLOWED_TRANSITIONS.put(WorkflowState.PAN_VALIDATION_PENDING, Set.of(WorkflowState.EKYC));
        ALLOWED_TRANSITIONS.put(WorkflowState.EKYC, Set.of(WorkflowState.EKYC_PENDING));
        ALLOWED_TRANSITIONS.put(WorkflowState.EKYC_PENDING, Set.of(WorkflowState.VKYC));
        ALLOWED_TRANSITIONS.put(WorkflowState.VKYC, Set.of(WorkflowState.VKYC_PENDING));
        ALLOWED_TRANSITIONS.put(WorkflowState.VKYC_PENDING, Set.of(WorkflowState.LIMIT_CREATION));
        ALLOWED_TRANSITIONS.put(WorkflowState.LIMIT_CREATION, Set.of(WorkflowState.LIMIT_CREATION_PENDING));
        ALLOWED_TRANSITIONS.put(WorkflowState.LIMIT_CREATION_PENDING, Set.of(WorkflowState.OFFER_GENERATED));
        // OFFER_GENERATED is terminal and has no next states.
    }

    /**
     * Validates and performs a workflow state transition.
     *
     * @param previousState current/previous workflow state
     * @param nextState     desired next workflow state
     * @return the nextState if the transition is allowed
     * @throws InvalidWorkflowTransitionException if the transition is not allowed
     *                                            or states are null
     */
    public static WorkflowState transitionTo(WorkflowState previousState, WorkflowState nextState) {
        if (previousState == null || nextState == null) {
            throw new InvalidWorkflowTransitionException("Both previousState and nextState must be non-null.");
        }

        Set<WorkflowState> allowed = ALLOWED_TRANSITIONS.getOrDefault(previousState, Collections.emptySet());
        if (!allowed.contains(nextState)) {
            throw new InvalidWorkflowTransitionException(previousState, nextState, allowed);
        }

        return nextState;
    }

    /**
     * Checks if a transition from previousState to nextState is valid.
     */
    public static boolean isValidTransition(WorkflowState previousState, WorkflowState nextState) {
        if (previousState == null || nextState == null) {
            return false;
        }
        Set<WorkflowState> allowed = ALLOWED_TRANSITIONS.getOrDefault(previousState, Collections.emptySet());
        return allowed.contains(nextState);
    }

    /**
     * Returns the set of valid next states for a given state.
     */
    public static Set<WorkflowState> getAllowedNextStates(WorkflowState currentState) {
        if (currentState == null) {
            return Collections.emptySet();
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentState, Collections.emptySet());
    }
}
