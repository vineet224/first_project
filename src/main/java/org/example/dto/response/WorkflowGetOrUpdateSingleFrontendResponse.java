package org.example.dto.response;

import lombok.Data;
import org.example.dto.WorkflowState;
import org.example.dto.request.WorkflowStateRequestPayload;

@Data
public class WorkflowGetOrUpdateSingleFrontendResponse {
    private String workflowId;
    private WorkflowState workflowState;
    private String ekyc_url; // this belong the state when Pan is done already and user has to do the ekyc
                             // with perfios sdk on frontenc means EKYC state
    private String vkyc_url;// this belong to the state VKYC
    private String limit_alloted; // this is limit creation and at state Offer_created
}
