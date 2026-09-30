package org.example.model;

import lombok.Data;

@Data
public class VkycDocument implements DocumentDetails {
    private String vkyc_url;
    private String vkyc_token;
}
