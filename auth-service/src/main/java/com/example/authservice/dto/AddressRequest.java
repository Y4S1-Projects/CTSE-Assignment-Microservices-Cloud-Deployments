package com.example.authservice.dto;

import lombok.Data;

@Data
public class AddressRequest {
    private String id;
    private String label;
    private String street;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String formattedAddress;
    private Double latitude;
    private Double longitude;
    private String googlePlaceId;
    private String locationSource;
    private Boolean isDefault;

    public Boolean getDefault() {
        return this.isDefault;
    }

    public void setDefault(Boolean value) {
        this.isDefault = value;
    }
}
