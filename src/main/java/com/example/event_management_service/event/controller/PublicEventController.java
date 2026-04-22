package com.example.event_management_service.event.controller;

import com.example.event_management_service.event.dtos.EventListResponse;
import com.example.event_management_service.event.dtos.EventResponse;
import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.service.EventService;
import com.example.event_management_service.shared.model.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@Slf4j
public class PublicEventController {
    private static final String LOG_GROUP_BROWSE = "[PUBLIC_EVENT_CONTROLLER][BROWSE_EVENTS]";
    private static final String LOG_GROUP_GET_BY_ID = "[PUBLIC_EVENT_CONTROLLER][GET_EVENT_BY_ID]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private final EventService eventService;

    @Autowired
    public PublicEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<EventListResponse> browseEvents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            Pageable pageable
    ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info(
                "{} request: requestId={}, query={}, city={}, category={}, startDate={}, endDate={}, page={}, size={}",
                LOG_GROUP_BROWSE, requestId, query, city, category, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize()
        );
        EventListResponse response = new EventListResponse();
        try {
            Page<Event> events = eventService.searchPublicEvents(query, startDate, endDate, city, category, pageable);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            response.setMessage("Events fetched successfully");
            response.setEvents(events.getContent());
            response.setTotalElements(events.getTotalElements());
            response.setTotalPages(events.getTotalPages());
            response.setPageNumber(events.getNumber());
            response.setPageSize(events.getSize());
            log.info(
                    "{} success: requestId={}, totalElements={}, totalPages={}, latencyMs={}",
                    LOG_GROUP_BROWSE, requestId, events.getTotalElements(), events.getTotalPages(), elapsedMillis(startNanos)
            );
        } catch (Exception ex) {
            response.setResponseStatus(ResponseStatus.FAILURE);
            response.setMessage(ex.getMessage());
            log.error("{} failure: requestId={}, latencyMs={}", LOG_GROUP_BROWSE, requestId, elapsedMillis(startNanos), ex);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID eventId) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info("{} request: requestId={}, eventId={}", LOG_GROUP_GET_BY_ID, requestId, eventId);
        EventResponse response = new EventResponse();
        try {
            Event event = eventService.getEventById(eventId);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            response.setMessage("Event fetched successfully");
            response.setEvent(event);
            log.info("{} success: requestId={}, eventId={}, latencyMs={}", LOG_GROUP_GET_BY_ID, requestId, eventId, elapsedMillis(startNanos));
        } catch (EventNotFoundException ex) {
            response.setResponseStatus(ResponseStatus.FAILURE);
            response.setMessage(ex.getMessage());
            log.warn(
                    "{} failure: requestId={}, eventId={}, reason={}, latencyMs={}",
                    LOG_GROUP_GET_BY_ID, requestId, eventId, ex.getMessage(), elapsedMillis(startNanos)
            );
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
        return ResponseEntity.ok(response);
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
