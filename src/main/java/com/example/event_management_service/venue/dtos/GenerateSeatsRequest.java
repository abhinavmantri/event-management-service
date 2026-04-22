package com.example.event_management_service.venue.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class GenerateSeatsRequest {
    @Min(value = 1, message = "rowCount must be greater than 0")
    private int rowCount;

    @Min(value = 1, message = "seatsPerRow must be greater than 0")
    private int seatsPerRow;

    @Min(value = 0, message = "rowLabelType must be 0 (NUMERIC) or 1 (ALPHA)")
    @Max(value = 1, message = "rowLabelType must be 0 (NUMERIC) or 1 (ALPHA)")
    private int rowLabelType;

    @Min(value = 1, message = "startSeatNumber must be greater than 0")
    private int startSeatNumber;
}
