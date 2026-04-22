package com.example.event_management_service.shared.model;

import lombok.Data;

@Data
public class ApiResponse {
    private ResponseStatus responseStatus;
    private String message;
}
