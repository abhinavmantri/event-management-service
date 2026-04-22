package com.example.event_management_service.event.exceptions;

public class EventExistsException extends RuntimeException{
    public EventExistsException(String message) {
        super(message);
    }
}
