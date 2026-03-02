package com.example.event_management_service.venue.exceptions;

public class VenueExistsException extends RuntimeException {
    public VenueExistsException(String message) {
        super(message);
    }
}
