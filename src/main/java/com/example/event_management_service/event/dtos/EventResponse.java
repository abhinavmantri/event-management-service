package com.example.event_management_service.event.dtos;

import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.shared.model.ApiResponse;

import lombok.Data;

@Data
public class EventResponse extends ApiResponse {
    private Event event;
}
