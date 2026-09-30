package org.example.vegapay.response;

import lombok.Data;
import org.example.vegapay.VegapayWorkflowState;
import org.example.vegapay.VegapayWorkflowStateStatus;

@Data
public class VegapayGeneralResponse {

    public enum EPanUpdateStatus {
        Accepted,
        Declined
    }

    String applicationId;
    VegapayWorkflowState workflowState;
    VegapayWorkflowStateStatus workflowStateStatus;
    String ekycUrl; // only available when the state is after the ekyc
    String limitCreated; // only available when the state is after the limit generation
    String videoKycSessionUrl;
    EPanUpdateStatus panUpdateStatus;
}
