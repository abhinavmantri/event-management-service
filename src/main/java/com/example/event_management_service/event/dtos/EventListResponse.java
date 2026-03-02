package com.example.event_management_service.event.dtos;

import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.shared.model.ApiResponse;

import lombok.Data;

import java.util.List;

@Data
public class EventListResponse extends ApiResponse {
    private Long totalElements;
    private int totalPages;
    private List<Event> events;
    private int pageNumber;
    private int pageSize;
}
