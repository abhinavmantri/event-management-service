package com.example.event_management_service.venue.exceptions;

public class VenueSectionExistsException extends RuntimeException {
    public VenueSectionExistsException(String message) {
        super(message);
    }
}
