package com.example.event_management_service.event.controller;

import com.example.event_management_service.event.dtos.CreateEventRequest;
import com.example.event_management_service.event.dtos.CreateEventResponse;
import com.example.event_management_service.event.dtos.EventPricingResponse;
import com.example.event_management_service.event.dtos.EventPricingRequest;
import com.example.event_management_service.event.dtos.EventSeatInventory;
import com.example.event_management_service.event.dtos.UpdateEventResponse;
import com.example.event_management_service.event.dtos.UpdateEventRequest;
import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.exceptions.InvalidEventStateException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.service.OrganiserEventService;
import com.example.event_management_service.shared.model.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganiserEventControllerTest {

    @Mock
    private OrganiserEventService organiserEventService;

    @InjectMocks
    private OrganiserEventController organiserEventController;

    @Test
    void createEventSuccessReturnsCreated() {
        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        CreateEventRequest request = new CreateEventRequest();
        request.setVenueId(venueId);
        request.setTitle("Rock Night");
        request.setStartsAt(Instant.now().plusSeconds(3600));

        Map<String, Object> claims = Map.of("id", UUID.randomUUID().toString(), "email", "org@example.com");
        UUID organiserId = UUID.fromString(claims.get("id").toString());
        Event savedEvent = new Event();
        savedEvent.setId(eventId);
        savedEvent.setTitle("Rock Night");

        when(organiserEventService.createEvent(request, organiserId, "org@example.com")).thenReturn(savedEvent);

        ResponseEntity<?> response = organiserEventController.createEvent(request, claims);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, ((CreateEventResponse) response.getBody()).getResponseStatus());
        assertSame(savedEvent, ((CreateEventResponse) response.getBody()).getEvent());
        verify(organiserEventService).createEvent(request, organiserId, "org@example.com");
    }

    @Test
    void updateEventNotFoundReturnsNotFound() {
        UUID eventId = UUID.randomUUID();
        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Updated");
        Map<String, Object> claims = Map.of("id", UUID.randomUUID().toString(), "email", "org@example.com");
        UUID organiserId = UUID.fromString(claims.get("id").toString());

        when(organiserEventService.updateEvent(eventId, request, organiserId)).thenThrow(new EventNotFoundException("Event not found"));

        ResponseEntity<?> response = organiserEventController.updateEvent(eventId, request, claims);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, ((UpdateEventResponse) response.getBody()).getResponseStatus());
        assertEquals("Event not found", ((UpdateEventResponse) response.getBody()).getMessage());
    }

    @Test
    void configureEventPricingBadRequestOnInvalidState() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> claims = Map.of("id", UUID.randomUUID().toString(), "email", "org@example.com");
        UUID organiserId = UUID.fromString(claims.get("id").toString());
        EventPricingRequest request = new EventPricingRequest();
        request.setCurrency("INR");
        EventPricingRequest.PriceItem priceItem = new EventPricingRequest.PriceItem();
        priceItem.setSectionId(UUID.randomUUID());
        priceItem.setPriceCents(5000);
        request.setPrices(List.of(priceItem));

        when(organiserEventService.configureEventPricing(eventId, request, organiserId))
                .thenThrow(new InvalidEventStateException("Pricing can be configured only for draft events"));

        ResponseEntity<?> response = organiserEventController.configureEventPricing(eventId, request, claims);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ResponseStatus.FAILURE, ((EventPricingResponse) response.getBody()).getResponseStatus());
    }

    @Test
    void initializeInventorySuccessMarksAlreadyInitializedWhenZeroSeatsCreated() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> claims = Map.of("id", UUID.randomUUID().toString(), "email", "org@example.com");
        UUID organiserId = UUID.fromString(claims.get("id").toString());
        when(organiserEventService.initializeEventInventory(eventId, organiserId)).thenReturn(0L);

        ResponseEntity<EventSeatInventory> response = organiserEventController.initializeEventInventory(eventId, claims);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResponseStatus.SUCCESS, response.getBody().getResponseStatus());
        assertEquals(eventId, response.getBody().getEventId());
        assertEquals(0L, response.getBody().getCreatedSeats());
        assertTrue(response.getBody().isAlreadyInitialized());
    }
}
