package com.example.event_management_service.event.controller;

import com.example.event_management_service.event.dtos.EventListResponse;
import com.example.event_management_service.event.dtos.EventResponse;
import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.service.EventService;
import com.example.event_management_service.shared.model.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void browseEventsSuccessReturnsPagedEventList() {
        String query = "music";
        String city = "Delhi";
        String category = "Concert";
        Instant startDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2026-12-31T23:59:59Z");
        Pageable pageable = PageRequest.of(0, 2);

        Event event1 = new Event();
        Event event2 = new Event();
        Page<Event> eventsPage = new PageImpl<>(List.of(event1, event2), pageable, 5);

        when(eventService.searchPublicEvents(query, startDate, endDate, city, category, pageable))
                .thenReturn(eventsPage);

        ResponseEntity<EventListResponse> responseEntity = eventController.browseEvents(
                query, city, category, startDate, endDate, pageable
        );

        EventListResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.SUCCESS, response.getResponseStatus());
        assertEquals("Events fetched successfully", response.getMessage());
        assertEquals(5L, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(0, response.getPageNumber());
        assertEquals(2, response.getPageSize());
        assertEquals(2, response.getEvents().size());
        verify(eventService).searchPublicEvents(query, startDate, endDate, city, category, pageable);
    }

    @Test
    void browseEventsFailureReturnsFailureResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(eventService.searchPublicEvents(null, null, null, null, null, pageable))
                .thenThrow(new RuntimeException("Search failed"));

        ResponseEntity<EventListResponse> responseEntity = eventController.browseEvents(
                null, null, null, null, null, pageable
        );

        EventListResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.FAILURE, response.getResponseStatus());
        assertEquals("Search failed", response.getMessage());
    }

    @Test
    void getEventByIdSuccessReturnsEvent() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        when(eventService.getEventById(eventId)).thenReturn(event);

        ResponseEntity<EventResponse> responseEntity = eventController.getEventById(eventId);

        EventResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.SUCCESS, response.getResponseStatus());
        assertEquals("Event fetched successfully", response.getMessage());
        assertSame(event, response.getEvent());
        verify(eventService).getEventById(eventId);
    }

    @Test
    void getEventByIdNotFoundReturnsFailureResponse() {
        UUID eventId = UUID.randomUUID();
        when(eventService.getEventById(eventId))
                .thenThrow(new EventNotFoundException("Event not found"));

        ResponseEntity<EventResponse> responseEntity = eventController.getEventById(eventId);

        EventResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.FAILURE, response.getResponseStatus());
        assertEquals("Event not found", response.getMessage());
    }
}
