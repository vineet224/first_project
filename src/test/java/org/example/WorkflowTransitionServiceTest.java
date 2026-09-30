package org.example;

import org.example.dto.WorkflowState;
import org.example.exception.InvalidWorkflowTransitionException;
import org.example.service.WorkflowTransitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowTransitionServiceTest {

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(WorkflowState.CUSTOMER_ONBOARDING, WorkflowState.PAN_VALIDATION),
                Arguments.of(WorkflowState.PAN_VALIDATION, WorkflowState.PAN_VALIDATION_PENDING),
                Arguments.of(WorkflowState.PAN_VALIDATION_PENDING, WorkflowState.EKYC),
                Arguments.of(WorkflowState.EKYC, WorkflowState.EKYC_PENDING),
                Arguments.of(WorkflowState.EKYC_PENDING, WorkflowState.VKYC),
                Arguments.of(WorkflowState.VKYC, WorkflowState.VKYC_PENDING),
                Arguments.of(WorkflowState.VKYC_PENDING, WorkflowState.LIMIT_CREATION),
                Arguments.of(WorkflowState.LIMIT_CREATION, WorkflowState.LIMIT_CREATION_PENDING),
                Arguments.of(WorkflowState.LIMIT_CREATION_PENDING, WorkflowState.OFFER_GENERATED)
        );
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void testValidTransitionsViaService(WorkflowState from, WorkflowState to) {
        WorkflowState result = WorkflowTransitionService.transitionTo(from, to);
        assertEquals(to, result);

        // Also test exact case TransitionTO alias
        WorkflowState aliasResult = WorkflowTransitionService.TransitionTO(from, to);
        assertEquals(to, aliasResult);

        // Also test via instance method on enum
        WorkflowState instanceResult = from.transitionTo(to);
        assertEquals(to, instanceResult);

        // Also test isValidTransition helper
        assertTrue(WorkflowTransitionService.isValidTransition(from, to));
        assertTrue(from.canTransitionTo(to));
    }

    @Test
    void testInvalidSkipStepTransitionThrows() {
        assertThrows(InvalidWorkflowTransitionException.class, () ->
                WorkflowTransitionService.transitionTo(WorkflowState.CUSTOMER_ONBOARDING, WorkflowState.EKYC));

        assertThrows(InvalidWorkflowTransitionException.class, () ->
                WorkflowState.transitionTo(WorkflowState.CUSTOMER_ONBOARDING, WorkflowState.OFFER_GENERATED));
    }

    @Test
    void testInvalidBackwardTransitionThrows() {
        assertThrows(InvalidWorkflowTransitionException.class, () ->
                WorkflowTransitionService.transitionTo(WorkflowState.PAN_VALIDATION, WorkflowState.CUSTOMER_ONBOARDING));
    }

    @Test
    void testInvalidTerminalStateTransitionThrows() {
        assertThrows(InvalidWorkflowTransitionException.class, () ->
                WorkflowState.OFFER_GENERATED.transitionTo(WorkflowState.CUSTOMER_ONBOARDING));
    }

    @Test
    void testSelfTransitionThrows() {
        assertThrows(InvalidWorkflowTransitionException.class, () ->
                WorkflowState.PAN_VALIDATION.transitionTo(WorkflowState.PAN_VALIDATION));
    }

    @Test
    void testNullStateThrows() {
        assertThrows(InvalidWorkflowTransitionException.class, () ->
                WorkflowTransitionService.transitionTo(null, WorkflowState.PAN_VALIDATION));

        assertThrows(InvalidWorkflowTransitionException.class, () ->
                WorkflowTransitionService.transitionTo(WorkflowState.CUSTOMER_ONBOARDING, null));
    }

    @Test
    void testAllowedNextStates() {
        assertTrue(WorkflowState.CUSTOMER_ONBOARDING.getAllowedNextStates().contains(WorkflowState.PAN_VALIDATION));
        assertEquals(1, WorkflowState.CUSTOMER_ONBOARDING.getAllowedNextStates().size());
        assertTrue(WorkflowState.OFFER_GENERATED.getAllowedNextStates().isEmpty());
    }
}
