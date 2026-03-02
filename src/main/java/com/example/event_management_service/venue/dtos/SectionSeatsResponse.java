package com.example.event_management_service.venue.dtos;

import com.example.event_management_service.shared.model.ApiResponse;
import com.example.event_management_service.venue.model.VenueSeat;
import lombok.Data;

import java.util.List;

@Data
public class SectionSeatsResponse extends ApiResponse {
    private List<VenueSeat> seats;
}
