package com.example.event_management_service.venue.controller;

import com.example.event_management_service.venue.dtos.CreateVenueRequest;
import com.example.event_management_service.venue.dtos.CreateVenueResponse;
import com.example.event_management_service.shared.model.ResponseStatus;
import com.example.event_management_service.venue.dtos.VenueListResponse;
import com.example.event_management_service.venue.dtos.VenueResponse;
import com.example.event_management_service.venue.exceptions.VenueExistsException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.model.Venue;
import com.example.event_management_service.venue.service.VenueService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/venues")
@Slf4j
public class VenueController {
    private static final String LOG_GROUP_CREATE = "[VENUE_CONTROLLER][CREATE_VENUE]";
    private static final String LOG_GROUP_GET_ALL = "[VENUE_CONTROLLER][GET_ALL_VENUES]";
    private static final String LOG_GROUP_GET_BY_ID = "[VENUE_CONTROLLER][GET_VENUE_BY_ID]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<CreateVenueResponse> createVenue(@RequestBody @Valid CreateVenueRequest createVenueRequest) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info("{} request: requestId={}", LOG_GROUP_CREATE, requestId);
        CreateVenueResponse response = new CreateVenueResponse();
        try {
            Venue savedVenue = venueService.createVenue(
                    createVenueRequest.getName(),
                    createVenueRequest.getCity(),
                    createVenueRequest.getAddress()
            );
            response.setVenue(savedVenue);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            response.setMessage("Venue created successfully");
            log.info(
                    "{} success: requestId={}, venueId={}, latencyMs={}",
                    LOG_GROUP_CREATE, requestId, savedVenue.getId(), elapsedMillis(startNanos)
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (VenueExistsException ex) {
            response.setResponseStatus(ResponseStatus.FAILURE);
            response.setMessage(ex.getMessage());
            log.warn(
                    "{} failure: requestId={}, reason={}, latencyMs={}",
                    LOG_GROUP_CREATE, requestId, ex.getMessage(), elapsedMillis(startNanos)
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    @GetMapping
    public ResponseEntity<VenueListResponse> getAllVenues(
            @RequestParam(required = false) String query,
            Pageable pageable
    ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info(
                "{} request: requestId={}, query={}, page={}, size={}",
                LOG_GROUP_GET_ALL, requestId, query, pageable.getPageNumber(), pageable.getPageSize()
        );
        try {
            Page<Venue> venues = venueService.searchVenues(query, pageable);
            VenueListResponse responseBody = new VenueListResponse();
            responseBody.setVenues(venues.getContent());
            responseBody.setPage(venues.getNumber());
            responseBody.setPageSize(venues.getSize());
            responseBody.setTotalCount((int) venues.getTotalElements());
            responseBody.setResponseStatus(ResponseStatus.SUCCESS);
            responseBody.setMessage("Venues fetched successfully");
            ResponseEntity<VenueListResponse> response = ResponseEntity.ok(responseBody);
            log.info("{} success: requestId={}, latencyMs={}", LOG_GROUP_GET_ALL, requestId, elapsedMillis(startNanos));
            return response;
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<VenueResponse> getVenueById(@PathVariable UUID venueId) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info("{} request: requestId={}, venueId={}", LOG_GROUP_GET_BY_ID, requestId, venueId);
        VenueResponse response = new VenueResponse();
        try {
            Venue venue = venueService.getVenueById(venueId);
            response.setVenue(venue);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            response.setMessage("Venue fetched successfully");
            log.info(
                    "{} success: requestId={}, venueId={}, latencyMs={}",
                    LOG_GROUP_GET_BY_ID, requestId, venueId, elapsedMillis(startNanos)
            );
        } catch (VenueNotFoundException ex) {
            response.setResponseStatus(ResponseStatus.FAILURE);
            response.setMessage(ex.getMessage());
            log.warn(
                    "{} failure: requestId={}, venueId={}, reason={}, latencyMs={}",
                    LOG_GROUP_GET_BY_ID, requestId, venueId, ex.getMessage(), elapsedMillis(startNanos)
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
        return ResponseEntity.ok(response);
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
