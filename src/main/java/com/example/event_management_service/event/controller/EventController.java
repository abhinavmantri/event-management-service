package com.example.event_management_service.event.controller;

import com.example.event_management_service.event.dtos.EventListResponse;
import com.example.event_management_service.event.dtos.EventResponse;
import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.service.EventService;
import com.example.event_management_service.shared.model.ResponseStatus;

import lombok.extern.slf4j.Slf4j;

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
@RequestMapping("/events")
@Slf4j
public class EventController {
    private static final String LOG_GROUP_BROWSE = "[EVENT_CONTROLLER][BROWSE_EVENTS]";
    private static final String LOG_GROUP_GET_BY_ID = "[EVENT_CONTROLLER][GET_EVENT_BY_ID]";
    private final EventService eventService;

    @Autowired
    public EventController(EventService eventService) {
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
        long startNanos = System.nanoTime();
        log.info(
                "{} request: query={}, city={}, category={}, startDate={}, endDate={}, page={}, size={}",
                LOG_GROUP_BROWSE, query, city, category, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize()
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
                    "{} success: totalElements={}, totalPages={}, latencyMs={}",
                    LOG_GROUP_BROWSE, events.getTotalElements(), events.getTotalPages(), elapsedMillis(startNanos)
            );
        } catch (Exception ex) {
            response.setResponseStatus(ResponseStatus.FAILURE);
            response.setMessage(ex.getMessage());
            log.error("{} failure: latencyMs={}", LOG_GROUP_BROWSE, elapsedMillis(startNanos), ex);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID eventId) {
        long startNanos = System.nanoTime();
        log.info("{} request: eventId={}", LOG_GROUP_GET_BY_ID, eventId);
        EventResponse response = new EventResponse();
        try {
            Event event = eventService.getEventById(eventId);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            response.setMessage("Event fetched successfully");
            response.setEvent(event);
            log.info("{} success: eventId={}, latencyMs={}", LOG_GROUP_GET_BY_ID, eventId, elapsedMillis(startNanos));
        } catch (EventNotFoundException ex) {
            response.setResponseStatus(ResponseStatus.FAILURE);
            response.setMessage(ex.getMessage());
            log.warn(
                    "{} failure: eventId={}, reason={}, latencyMs={}",
                    LOG_GROUP_GET_BY_ID, eventId, ex.getMessage(), elapsedMillis(startNanos)
            );
        }
        return ResponseEntity.ok(response);
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
