package com.example.event_management_service.event.service;

import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventStatus;
import com.example.event_management_service.event.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class EventService {
    private static final String LOG_GROUP_SEARCH = "[EVENT_SERVICE][SEARCH_PUBLIC_EVENTS]";
    private static final String LOG_GROUP_GET_BY_ID = "[EVENT_SERVICE][GET_EVENT_BY_ID]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Page<Event> searchPublicEvents(
            String query,
            Instant startDate,
            Instant endDate,
            String city,
            String category,
            Pageable pageable
    ) {
        String requestId = requestId();
        log.info(
                "{} request: requestId={}, query={}, city={}, category={}, startDate={}, endDate={}, page={}, size={}",
                LOG_GROUP_SEARCH, requestId, query, city, category, startDate, endDate, pageable.getPageNumber(), pageable.getPageSize()
        );
        Instant now = Instant.now();
        Pageable effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        String normalizedQuery = normalizeText(query);
        String normalizedCity = normalizeText(city);
        String normalizedCategory = normalizeText(category);

        Page<Event> events = eventRepository.searchPublicEvents(
                EventStatus.PUBLISHED,
                now,
                startDate,
                endDate,
                normalizedCity,
                normalizedCategory,
                normalizedQuery,
                effectivePageable
        );
        log.info(
                "{} success: requestId={}, resultCount={}, totalElements={}",
                LOG_GROUP_SEARCH, requestId, events.getNumberOfElements(), events.getTotalElements()
        );
        return events;
    }

    public Event getEventById(UUID eventId) throws EventNotFoundException {
        String requestId = requestId();
        log.info("{} request: requestId={}, eventId={}", LOG_GROUP_GET_BY_ID, requestId, eventId);
        Event event = eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED)
                .orElseThrow(() -> {
                    log.warn("{} failure: requestId={}, eventId={}, reason=Published event not found", LOG_GROUP_GET_BY_ID, requestId, eventId);
                    return new EventNotFoundException("Event not found");
                });
        log.info("{} success: requestId={}, eventId={}", LOG_GROUP_GET_BY_ID, requestId, eventId);
        return event;
    }

    private String normalizeText(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return input.trim();
    }

    private String requestId() {
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        return requestId == null ? "N/A" : requestId;
    }
}
