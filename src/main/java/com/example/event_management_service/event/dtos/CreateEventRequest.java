package com.example.event_management_service.event.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CreateEventRequest {
    @NotNull(message = "venueId is required")
    private UUID venueId;

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    private String description;

    @Size(max = 60, message = "category must be at most 60 characters")
    private String category;

    @NotNull(message = "startsAt is required")
    @Future(message = "startsAt must be a future date-time")
    private Instant startsAt;

    @Future(message = "endsAt must be a future date-time")
    private Instant endsAt;

    @AssertTrue(message = "endsAt must be greater than or equal to startsAt")
    public boolean isEndsAtValid() {
        if (endsAt == null || startsAt == null) {
            return true;
        }
        return !endsAt.isBefore(startsAt);
    }
}
