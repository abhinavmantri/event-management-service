package com.example.event_management_service.venue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController {

    @PostMapping
    public ResponseEntity<?> createVenue(@RequestBody Map<String, Object> request) {
        // TODO: implement venue creation for ADMIN/ORGANIZER
        return ResponseEntity.ok(Map.of("message", "TODO: create venue"));
    }

    @GetMapping
    public ResponseEntity<?> getAllVenues() {
        // TODO: implement venue listing
        return ResponseEntity.ok(Map.of("message", "TODO: get all venues"));
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<?> getVenueById(@PathVariable Long venueId) {
        // TODO: implement venue retrieval by id
        return ResponseEntity.ok(Map.of("message", "TODO: get venue by id", "venueId", venueId));
    }
}
