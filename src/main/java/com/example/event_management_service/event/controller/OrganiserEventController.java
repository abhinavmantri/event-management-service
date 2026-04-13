package com.example.event_management_service.event.controller;

import com.example.event_management_service.event.dtos.*;
import com.example.event_management_service.event.exceptions.EventExistsException;
import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.exceptions.InvalidEventStateException;
import com.example.event_management_service.event.filter.OrganiserAuthorizationFilter;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventSectionPricing;
import com.example.event_management_service.event.service.OrganiserEventService;
import com.example.event_management_service.shared.model.ApiResponse;
import com.example.event_management_service.shared.model.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/organiser/events")
@Slf4j
public class OrganiserEventController {
    private static final String LOG_GROUP_CREATE = "[ORGANISER_EVENT_CONTROLLER][CREATE_EVENT]";
    private static final String LOG_GROUP_UPDATE = "[ORGANISER_EVENT_CONTROLLER][UPDATE_EVENT]";
    private static final String LOG_GROUP_PUBLISH = "[ORGANISER_EVENT_CONTROLLER][PUBLISH_EVENT]";
    private static final String LOG_GROUP_PRICING = "[ORGANISER_EVENT_CONTROLLER][CONFIGURE_PRICING]";
    private static final String LOG_GROUP_INVENTORY = "[ORGANISER_EVENT_CONTROLLER][INITIALIZE_INVENTORY]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private final OrganiserEventService organiserEventService;

    @Autowired
    public OrganiserEventController(OrganiserEventService organiserEventService) {
        this.organiserEventService = organiserEventService;
    }

    @PostMapping
    public ResponseEntity<CreateEventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestAttribute(OrganiserAuthorizationFilter.ORGANISER_CLAIMS_ATTRIBUTE) Map<String, Object> claims
    ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info("{} request: requestId={}, venueId={}, title={}", LOG_GROUP_CREATE, requestId, request.getVenueId(), request.getTitle());
        CreateEventResponse response = new CreateEventResponse();
        try {
            UUID organiserId = requireOrganiserId(claims);
            String organiserEmail = requireClaimAsText(claims, "email");
            Event event = organiserEventService.createEvent(request, organiserId, organiserEmail);
            
            response.setMessage("Event created successfully");
            response.setEvent(event);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info("{} success: requestId={}, eventId={}, latencyMs={}", LOG_GROUP_CREATE, requestId, event.getId(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, reason={}, latencyMs={}", LOG_GROUP_CREATE, requestId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventExistsException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, reason={}, latencyMs={}", LOG_GROUP_CREATE, requestId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<UpdateEventResponse> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestAttribute(OrganiserAuthorizationFilter.ORGANISER_CLAIMS_ATTRIBUTE) Map<String, Object> claims
    ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info("{} request: requestId={}, eventId={}", LOG_GROUP_UPDATE, requestId, eventId);
        UpdateEventResponse response = new UpdateEventResponse();
        try {
            Event updatedEvent = organiserEventService.updateEvent(eventId, request, requireOrganiserId(claims));
            response.setMessage("Event updated successfully");
            response.setEvent(updatedEvent);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info("{} success: requestId={}, eventId={}, latencyMs={}", LOG_GROUP_UPDATE, requestId, eventId, elapsedMillis(startNanos));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, eventId={}, reason={}, latencyMs={}", LOG_GROUP_UPDATE, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventNotFoundException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, eventId={}, reason={}, latencyMs={}", LOG_GROUP_UPDATE, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    @PostMapping("/{eventId}/publish")
    public ResponseEntity<PublishEventResponse> publishEvent(
            @PathVariable UUID eventId,
            @RequestAttribute(OrganiserAuthorizationFilter.ORGANISER_CLAIMS_ATTRIBUTE) Map<String, Object> claims
    ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info("{} request: requestId={}, eventId={}", LOG_GROUP_PUBLISH, requestId, eventId);
        PublishEventResponse response = new PublishEventResponse();
        try {
            Event event = organiserEventService.publishEvent(eventId, requireOrganiserId(claims));
            response.setMessage("Event published successfully");
            response.setEventId(event.getId());
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info("{} success: requestId={}, eventId={}, latencyMs={}", LOG_GROUP_PUBLISH, requestId, eventId, elapsedMillis(startNanos));
            return ResponseEntity.ok(response);
        } catch (InvalidEventStateException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, eventId={}, reason={}, latencyMs={}", LOG_GROUP_PUBLISH, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventNotFoundException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, eventId={}, reason={}, latencyMs={}", LOG_GROUP_PUBLISH, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    @PostMapping("/{eventId}/pricing")
    public ResponseEntity<EventPricingResponse> configureEventPricing(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventPricingRequest request,
            @RequestAttribute(OrganiserAuthorizationFilter.ORGANISER_CLAIMS_ATTRIBUTE) Map<String, Object> claims
    ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        int priceItemsCount = request.getPrices() == null ? 0 : request.getPrices().size();
        log.info("{} request: requestId={}, eventId={}, priceItemsCount={}", LOG_GROUP_PRICING, requestId, eventId, priceItemsCount);
        EventPricingResponse response = new EventPricingResponse();
        try {
            List<EventSectionPricing> pricings = organiserEventService.configureEventPricing(eventId, request, requireOrganiserId(claims));
            response.setPricings(pricings);
            response.setMessage("Event pricing configured successfully");
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info("{} success: requestId={}, eventId={}, savedPricings={}, latencyMs={}", LOG_GROUP_PRICING, requestId, eventId, pricings.size(), elapsedMillis(startNanos));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | InvalidEventStateException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, eventId={}, reason={}, latencyMs={}", LOG_GROUP_PRICING, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventNotFoundException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, eventId={}, reason={}, latencyMs={}", LOG_GROUP_PRICING, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    @PostMapping("/{eventId}/inventory/init")
    public ResponseEntity<EventSeatInventory> initializeEventInventory(
            @PathVariable UUID eventId,
            @RequestAttribute(OrganiserAuthorizationFilter.ORGANISER_CLAIMS_ATTRIBUTE) Map<String, Object> claims
    ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info("{} request: requestId={}, eventId={}", LOG_GROUP_INVENTORY, requestId, eventId);
        EventSeatInventory response = new EventSeatInventory();
        try {
            long createdSeats = organiserEventService.initializeEventInventory(eventId, requireOrganiserId(claims));
            response.setEventId(eventId);
            response.setCreatedSeats(createdSeats);
            response.setAlreadyInitialized(createdSeats == 0);
            response.setMessage(createdSeats == 0
                    ? "Event inventory already initialized"
                    : "Event inventory initialized successfully");
            response.setResponseStatus(ResponseStatus.SUCCESS);
            log.info(
                    "{} success: requestId={}, eventId={}, createdSeats={}, alreadyInitialized={}, latencyMs={}",
                    LOG_GROUP_INVENTORY, requestId, eventId, createdSeats, createdSeats == 0, elapsedMillis(startNanos)
            );
            return ResponseEntity.ok(response);
        } catch (InvalidEventStateException | IllegalArgumentException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, eventId={}, reason={}, latencyMs={}", LOG_GROUP_INVENTORY, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (EventNotFoundException ex) {
            setErrorResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, eventId={}, reason={}, latencyMs={}", LOG_GROUP_INVENTORY, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private <T extends ApiResponse> void setErrorResponse(T response, String message) {
        response.setMessage(message);
        response.setResponseStatus(ResponseStatus.FAILURE);
    }

    private UUID requireOrganiserId(Map<String, Object> claims) {
        return UUID.fromString(requireClaimAsText(claims, "id"));
    }

    private String requireClaimAsText(Map<String, Object> claims, String claimName) {
        Object claimValue = claims.get(claimName);
        if (claimValue == null) {
            throw new IllegalArgumentException("Missing required claim: " + claimName);
        }
        String claimText = claimValue.toString().trim();
        if (claimText.isEmpty()) {
            throw new IllegalArgumentException("Missing required claim: " + claimName);
        }
        return claimText;
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
