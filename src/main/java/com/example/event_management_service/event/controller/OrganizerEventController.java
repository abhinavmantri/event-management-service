package com.example.event_management_service.event.controller;

import com.example.event_management_service.event.dtos.*;
import com.example.event_management_service.event.exceptions.EventExistsException;
import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.exceptions.InvalidEventStateException;
import com.example.event_management_service.event.filter.OrganizerAuthorizationFilter;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventSectionPricing;
import com.example.event_management_service.event.service.OrganizerEventService;
import com.example.event_management_service.shared.model.ApiResponse;
import com.example.event_management_service.shared.model.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/organizer/events")
@Slf4j
public class OrganizerEventController {
    private static final String LOG_GROUP_CREATE = "[ORGANIZER_EVENT_CONTROLLER][CREATE_EVENT]";
    private static final String LOG_GROUP_UPDATE = "[ORGANIZER_EVENT_CONTROLLER][UPDATE_EVENT]";
    private static final String LOG_GROUP_PUBLISH = "[ORGANIZER_EVENT_CONTROLLER][PUBLISH_EVENT]";
    private static final String LOG_GROUP_PRICING = "[ORGANIZER_EVENT_CONTROLLER][CONFIGURE_PRICING]";
    private static final String LOG_GROUP_INVENTORY = "[ORGANIZER_EVENT_CONTROLLER][INITIALIZE_INVENTORY]";
    private final OrganizerEventService organizerEventService;

    @Autowired
    public OrganizerEventController(OrganizerEventService organizerEventService) {
        this.organizerEventService = organizerEventService;
    }

    @PostMapping
    public ResponseEntity<CreateEventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestAttribute(OrganizerAuthorizationFilter.ORGANIZER_CLAIMS_ATTRIBUTE) Map<String, Object> claims
    ) {
        long startNanos = System.nanoTime();
        log.info("{} request: venueId={}, title={}", LOG_GROUP_CREATE, request.getVenueId(), request.getTitle());
        CreateEventResponse response = new CreateEventResponse();
        try {
            Event event = organizerEventService.createEvent(request, claims);
            
            response.setMessage("Event created successfully");
            response.setEvent(event);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info("{} success: eventId={}, latencyMs={}", LOG_GROUP_CREATE, event.getId(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: reason={}, latencyMs={}", LOG_GROUP_CREATE, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventExistsException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: reason={}, latencyMs={}", LOG_GROUP_CREATE, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<UpdateEventResponse> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        long startNanos = System.nanoTime();
        log.info("{} request: eventId={}", LOG_GROUP_UPDATE, eventId);
        UpdateEventResponse response = new UpdateEventResponse();
        try {
            Event updatedEvent = organizerEventService.updateEvent(eventId, request);
            response.setMessage("Event updated successfully");
            response.setEvent(updatedEvent);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info("{} success: eventId={}, latencyMs={}", LOG_GROUP_UPDATE, eventId, elapsedMillis(startNanos));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: eventId={}, reason={}, latencyMs={}", LOG_GROUP_UPDATE, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventNotFoundException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: eventId={}, reason={}, latencyMs={}", LOG_GROUP_UPDATE, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<PublishEventResponse> publishEvent(@PathVariable UUID eventId) {
        long startNanos = System.nanoTime();
        log.info("{} request: eventId={}", LOG_GROUP_PUBLISH, eventId);
        PublishEventResponse response = new PublishEventResponse();
        try {
            Event event = organizerEventService.publishEvent(eventId);
            response.setMessage("Event published successfully");
            response.setEventId(event.getId());
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info("{} success: eventId={}, latencyMs={}", LOG_GROUP_PUBLISH, eventId, elapsedMillis(startNanos));
            return ResponseEntity.ok(response);
        } catch (InvalidEventStateException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: eventId={}, reason={}, latencyMs={}", LOG_GROUP_PUBLISH, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventNotFoundException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: eventId={}, reason={}, latencyMs={}", LOG_GROUP_PUBLISH, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/{eventId}/pricing")
    public ResponseEntity<EventPricingResponse> configureEventPricing(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventPricingRequest request
    ) {
        long startNanos = System.nanoTime();
        int priceItemsCount = request.getPrices() == null ? 0 : request.getPrices().size();
        log.info("{} request: eventId={}, priceItemsCount={}", LOG_GROUP_PRICING, eventId, priceItemsCount);
        EventPricingResponse response = new EventPricingResponse();
        try {
            List<EventSectionPricing> pricings = organizerEventService.configureEventPricing(eventId, request);
            response.setPricings(pricings);
            response.setMessage("Event pricing configured successfully");
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info("{} success: eventId={}, savedPricings={}, latencyMs={}", LOG_GROUP_PRICING, eventId, pricings.size(), elapsedMillis(startNanos));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | InvalidEventStateException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: eventId={}, reason={}, latencyMs={}", LOG_GROUP_PRICING, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventNotFoundException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: eventId={}, reason={}, latencyMs={}", LOG_GROUP_PRICING, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/{eventId}/inventory/init")
    public ResponseEntity<EventSeatInventory> initializeEventInventory(@PathVariable UUID eventId) {
        long startNanos = System.nanoTime();
        log.info("{} request: eventId={}", LOG_GROUP_INVENTORY, eventId);
        EventSeatInventory response = new EventSeatInventory();
        try {
            long createdSeats = organizerEventService.initializeEventInventory(eventId);
            response.setEventId(eventId);
            response.setCreatedSeats(createdSeats);
            response.setAlreadyInitialized(createdSeats == 0);
            response.setMessage(createdSeats == 0
                    ? "Event inventory already initialized"
                    : "Event inventory initialized successfully");
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info(
                    "{} success: eventId={}, createdSeats={}, alreadyInitialized={}, latencyMs={}",
                    LOG_GROUP_INVENTORY, eventId, createdSeats, createdSeats == 0, elapsedMillis(startNanos)
            );
            return ResponseEntity.ok(response);
        } catch (InvalidEventStateException | IllegalArgumentException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: eventId={}, reason={}, latencyMs={}", LOG_GROUP_INVENTORY, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventNotFoundException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: eventId={}, reason={}, latencyMs={}", LOG_GROUP_INVENTORY, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    private <T extends ApiResponse> void setErrorResponse(T response, String message) {
        response.setMessage(message);
        response.setResponseStatus(ResponseStatus.FAILURE);
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
