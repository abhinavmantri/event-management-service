package com.example.event_management_service.venue.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateVenueRequest {
    @NotBlank(message = "Venue name is required")
    private String name;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Address is required")
    private String address;
}
