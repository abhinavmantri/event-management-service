package com.example.event_management_service.venue.dtos;

import com.example.event_management_service.shared.model.ApiResponse;
import com.example.event_management_service.venue.model.Venue;
import lombok.Data;

import java.util.List;

@Data
public class VenueListResponse extends ApiResponse {
    private List<Venue> venues;
    private int page;
    private int pageSize;
    private int totalCount;
}
