package com.example.authservice.dto;

import lombok.Data;

@Data
public class AddressRequest {
    private String street;
    private String city;
    private String postalCode;
    private boolean isDefault;
}
