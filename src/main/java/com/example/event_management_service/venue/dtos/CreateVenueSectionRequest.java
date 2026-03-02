package com.example.event_management_service.venue.dtos;

import lombok.Data;

@Data
public class CreateVenueSectionRequest {
    private String name;
    private int sortOrder;
}
