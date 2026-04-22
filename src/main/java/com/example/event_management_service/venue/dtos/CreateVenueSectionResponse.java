package com.example.event_management_service.venue.dtos;

import com.example.event_management_service.shared.model.ApiResponse;
import com.example.event_management_service.venue.model.VenueSection;
import lombok.Data;

@Data
public class CreateVenueSectionResponse extends ApiResponse {
    private VenueSection venueSection;
}
