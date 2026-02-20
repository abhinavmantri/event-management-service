package com.example.event_management_service.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
public class PublicEventController {

    @GetMapping
    public ResponseEntity<?> browseEvents() {
        // TODO: implement public event browse
        return ResponseEntity.ok(Map.of("message", "TODO: browse events"));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventById(@PathVariable Long eventId) {
        // TODO: implement event details fetch
        return ResponseEntity.ok(Map.of("message", "TODO: get event by id", "eventId", eventId));
    }
}
