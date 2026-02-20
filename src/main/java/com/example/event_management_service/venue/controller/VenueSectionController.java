package com.example.event_management_service.venue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/venues/{venueId}/sections")
public class VenueSectionController {

    @PostMapping
    public ResponseEntity<?> createVenueSection(
            @PathVariable Long venueId,
            @RequestBody Map<String, Object> request
    ) {
        // TODO: implement section creation for a venue
        return ResponseEntity.ok(Map.of("message", "TODO: create venue section", "venueId", venueId));
    }

    @PostMapping("/{sectionId}/seats/generate")
    public ResponseEntity<?> generateSectionSeats(
            @PathVariable Long venueId,
            @PathVariable Long sectionId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        // TODO: implement optional seat generation convenience endpoint
        return ResponseEntity.ok(
                Map.of(
                        "message", "TODO: generate section seats",
                        "venueId", venueId,
                        "sectionId", sectionId
                )
        );
    }
}
