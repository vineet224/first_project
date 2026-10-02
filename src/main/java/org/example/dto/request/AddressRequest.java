package org.example.dto.request;

import lombok.Data;

@Data 
public class AddressRequest {
    private String line1;
    private String street;
    private String city;
}
