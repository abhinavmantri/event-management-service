package com.example.event_management_service.event.dtos;

import java.util.UUID;

import com.example.event_management_service.shared.model.ApiResponse;

import lombok.Data;

@Data
public class PublishEventResponse extends ApiResponse {
    private UUID eventId;
}
