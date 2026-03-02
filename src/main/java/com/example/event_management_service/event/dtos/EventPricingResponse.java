package com.example.event_management_service.event.dtos;

import java.util.List;
import java.util.UUID;

import com.example.event_management_service.event.model.EventSectionPricing;
import com.example.event_management_service.shared.model.ApiResponse;
import lombok.Data;

@Data
public class EventPricingResponse extends ApiResponse {
    private UUID eventId;
    private List<EventSectionPricing> pricings;
}
