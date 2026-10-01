package org.example.service;

import org.example.dao.*;
import org.example.dto.WorkflowState;
import org.example.dto.request.*;
import org.example.model.entites.WorkflowEntity;
import org.example.vegapay.*;
import org.example.vegapay.client.VegapayClient;
import org.example.vegapay.response.*;
import org.example.vegapay.statehandlers.VegapayStateHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkflowProgressionTest {
    private final WorkflowMockRepository workflows = new WorkflowMockRepository();
    private final DocumentMockRepository documents = new DocumentMockRepository();
    private final VegapayClient client = mock(VegapayClient.class);
    private WorkflowEntity workflow;
    private WorkflowOrchestrator orchestrator;
    private VegapayProgressionService progression;

    @BeforeEach
    void setUp() {
        VegapayStateHandler remoteHandlers = new VegapayStateHandler(workflows, documents, client);
        progression = new VegapayProgressionService(workflows, client, remoteHandlers);
        WorkflowStateHandlers handlers = new WorkflowStateHandlers(workflows, documents,
                new WorkflowTransitionService(), remoteHandlers, client, progression);
        orchestrator = new WorkflowOrchestrator(new WorkflowResponseBuilder(documents),
                new UserWorkflowMappingRepository(), workflows, client, handlers, progression);
        workflow = new WorkflowEntity();
        workflow.setWorkflowId("workflow");
        workflow.setApplicationId("application");
        workflow.setWorkflowState(WorkflowState.PAN_VALIDATION);
        workflows.save(workflow);
    }

    private WorkflowUpdateRequest request() {
        WorkflowUpdateRequest request = new WorkflowUpdateRequest();
        request.setWorkflowId("workflow");
        request.setApplicationId("application");
        request.setWorkflowState(workflow.getWorkflowState());
        return request;
    }

    private VegapayStatusResponse status(VegapayWorkflowState state) {
        return new VegapayStatusResponse("application", state, VegapayWorkflowStateStatus.PNEDING);
    }

    @Test
    void acceptedPanSubmissionReturnsPreparedNextScreen() {
        when(client.getStatus("application")).thenReturn(
                status(VegapayWorkflowState.PAN_VERIFIVATION),
                status(VegapayWorkflowState.Ekyc_Url_Generated));
        VegapayGeneralResponse accepted = new VegapayGeneralResponse();
        accepted.setPanUpdateStatus(VegapayGeneralResponse.EPanUpdateStatus.Accepted);
        when(client.updatePanDetails("pan", "application", "dob")).thenReturn(accepted);
        VegapayGeneralResponse url = new VegapayGeneralResponse();
        url.setEkycUrl("https://example.test/ekyc");
        when(client.getEkycUrl("application")).thenReturn(url);
        PanDetailsRequest input = new PanDetailsRequest();
        input.setPanNo("pan");
        input.setDob("dob");
        WorkflowUpdateRequest request = request();
        request.setWorkflowStateRequestPayload(input);

        var response = orchestrator.updateWorkflow(request);

        assertEquals(WorkflowState.EKYC, response.getWorkflowState());
        assertEquals("https://example.test/ekyc", response.getEkyc_url());
        assertEquals(response.getEkyc_url(), orchestrator.getWorkflowFrontendResponse("workflow").getEkyc_url());
        verify(client, times(2)).getStatus("application");
    }

    @Test
    void screenEntryWithoutInputDoesNotSubmitPan() {
        assertEquals(WorkflowState.PAN_VALIDATION,
                orchestrator.updateWorkflow(request()).getWorkflowState());
        verifyNoInteractions(client);
    }

    @Test
    void pendingRequestStopsBeforeSalaryInput() {
        workflow.setWorkflowState(WorkflowState.VKYC_PENDING);
        when(client.getStatus("application")).thenReturn(status(VegapayWorkflowState.Limit_Generate));
        assertEquals(WorkflowState.LIMIT_CREATION,
                orchestrator.updateWorkflow(request()).getWorkflowState());
        verify(client).getStatus("application");
        verifyNoMoreInteractions(client);
    }

    @Test
    void intermediateActionDoesNotReadStatusAgain() {
        workflow.setWorkflowState(WorkflowState.EKYC_PENDING);
        when(client.getStatus("application")).thenReturn(status(VegapayWorkflowState.Ekyc_Verification));
        assertEquals(WorkflowState.EKYC_PENDING,
                orchestrator.updateWorkflow(request()).getWorkflowState());
        verify(client).getStatus("application");
        verify(client).callForEkycVerification("application");
        verifyNoMoreInteractions(client);
    }

    @Test
    void invalidBoundaryDoesNotCallVegapay() {
        workflow.setWorkflowState(WorkflowState.EKYC_PENDING);
        assertThrows(IllegalArgumentException.class, () -> progression.pollVegapay(
                VegapayWorkflowState.Offer_Generated, "workflow", "application"));
        verifyNoInteractions(client);
    }

    @Test
    void rejectionStopsProgressionRegardlessOfRemoteStatus() {
        workflow.setWorkflowState(WorkflowState.PAN_VALIDATION_PENDING);
        when(client.getStatus("application")).thenReturn(new VegapayStatusResponse(
                "application", VegapayWorkflowState.Application_Rejected, VegapayWorkflowStateStatus.FAILED));
        assertEquals(WorkflowState.REJECTED,
                orchestrator.updateWorkflow(request()).getWorkflowState());
        verify(client).getStatus("application");
        verifyNoMoreInteractions(client);
    }
}
