package com.example.event_management_service.venue.exceptions;

public class VenueNotFoundException extends RuntimeException {
    public VenueNotFoundException(String message) {
        super(message);
    }
}
