package com.example.event_management_service.event.dtos;

import com.example.event_management_service.shared.model.ApiResponse;
import lombok.Data;

import java.util.UUID;

@Data
public class EventSeatInventory extends ApiResponse {
     private UUID eventId;
     private long createdSeats;
     private boolean alreadyInitialized;
}
