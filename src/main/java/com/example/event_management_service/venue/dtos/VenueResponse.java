package com.example.event_management_service.venue.dtos;

import com.example.event_management_service.shared.model.ApiResponse;
import com.example.event_management_service.venue.model.Venue;
import lombok.Data;

@Data
public class VenueResponse extends ApiResponse {
    private Venue venue;
}
