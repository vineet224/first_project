package org.example.model;

import lombok.Data;

@Data
public class PanDocument implements DocumentDetails {
    private String panName;
    private String panNo;
    private String dob;
}
