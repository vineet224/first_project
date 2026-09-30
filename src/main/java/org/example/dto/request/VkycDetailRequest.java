package org.example.dto.request;

import lombok.Data;

@Data
public class VkycDetailRequest implements WorkflowStateRequestPayload {
    private String vkyc_session_token;
}
