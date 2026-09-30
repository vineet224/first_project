package org.example.vegapay.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.example.vegapay.VegapayWorkflowState;
import org.example.vegapay.VegapayWorkflowStateStatus;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VegapayStatusResponse {

    @JsonProperty("applicationId")
    private String applicationId;

    @JsonProperty("vegapayWorkflowState")
    private VegapayWorkflowState vegapayWorkflowState;

    @JsonProperty("vegapayWorkflowStateStatus")
    private VegapayWorkflowStateStatus vegapayWorkflowStateStatus;

    public VegapayStatusResponse() {
    }

    public VegapayStatusResponse(String applicationId, VegapayWorkflowState vegapayWorkflowState, VegapayWorkflowStateStatus vegapayWorkflowStateStatus) {
        this.applicationId = applicationId;
        this.vegapayWorkflowState = vegapayWorkflowState;
        this.vegapayWorkflowStateStatus = vegapayWorkflowStateStatus;
    }

    @Override
    public String toString() {
        return "VegapayStatusResponse{" +
                "applicationId='" + applicationId + '\'' +
                ", vegapayWorkflowState=" + vegapayWorkflowState +
                ", vegapayWorkflowStateStatus=" + vegapayWorkflowStateStatus +
                '}';
    }
}
