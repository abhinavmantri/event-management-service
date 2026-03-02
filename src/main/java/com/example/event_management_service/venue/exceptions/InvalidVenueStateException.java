package com.example.event_management_service.venue.exceptions;

public class InvalidVenueStateException extends RuntimeException {
    public InvalidVenueStateException(String message) {
        super(message);
    }
}
