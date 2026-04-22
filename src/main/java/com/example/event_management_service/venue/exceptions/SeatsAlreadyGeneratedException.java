package com.example.event_management_service.venue.exceptions;

public class SeatsAlreadyGeneratedException extends RuntimeException {
    public SeatsAlreadyGeneratedException(String message) {
        super(message);
    }
}
