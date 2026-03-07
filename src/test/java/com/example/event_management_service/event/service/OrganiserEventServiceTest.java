package com.example.event_management_service.event.service;

import com.example.event_management_service.event.dtos.CreateEventRequest;
import com.example.event_management_service.event.dtos.EventPricingRequest;
import com.example.event_management_service.event.dtos.UpdateEventRequest;
import com.example.event_management_service.event.exceptions.EventExistsException;
import com.example.event_management_service.event.exceptions.InvalidEventStateException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventStatus;
import com.example.event_management_service.event.repository.EventRepository;
import com.example.event_management_service.event.repository.EventSeatRepository;
import com.example.event_management_service.event.repository.EventSectionPricingRepository;
import com.example.event_management_service.venue.model.Venue;
import com.example.event_management_service.venue.model.VenueSection;
import com.example.event_management_service.venue.repository.VenueSeatRepository;
import com.example.event_management_service.venue.repository.VenueSectionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganiserEventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @Mock
    private EventSectionPricingRepository eventSectionPricingRepository;

    @Mock
    private VenueSeatRepository venueSeatRepository;

    @Mock
    private VenueSectionRepository venueSectionRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private OrganiserEventService organiserEventService;

    @Test
    void createEventNormalizesAndPersistsEvent() throws EventExistsException {
        UUID organiserId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Instant startsAt = Instant.now().plusSeconds(3600);

        CreateEventRequest request = new CreateEventRequest();
        request.setVenueId(venueId);
        request.setTitle("  Rock Night  ");
        request.setDescription("  Great show ");
        request.setCategory("  Music ");
        request.setStartsAt(startsAt);
        request.setEndsAt(startsAt.plusSeconds(7200));

        Map<String, Object> claims = Map.of(
                "id", organiserId.toString(),
                "email", " organiser@example.com "
        );

        Venue venueRef = new Venue();
        when(entityManager.getReference(Venue.class, venueId)).thenReturn(venueRef);
        when(eventRepository.existsByOrganiserIdAndVenue_IdAndTitleIgnoreCaseAndStartsAt(
                organiserId, venueId, "Rock Night", startsAt
        )).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            return event;
        });

        Event saved = organiserEventService.createEvent(request, claims);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event persisted = captor.getValue();

        assertEquals("Rock Night", persisted.getTitle());
        assertEquals("Great show", persisted.getDescription());
        assertEquals("Music", persisted.getCategory());
        assertEquals(EventStatus.DRAFT, persisted.getStatus());
        assertEquals(organiserId, persisted.getOrganiserId());
        assertEquals("organiser@example.com", persisted.getOrganiserEmail());
        assertSame(venueRef, persisted.getVenue());
        assertEquals(saved.getId(), persisted.getId());
    }

    @Test
    void createEventThrowsWhenDuplicateExists() {
        UUID organiserId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Instant startsAt = Instant.now().plusSeconds(3600);

        CreateEventRequest request = new CreateEventRequest();
        request.setVenueId(venueId);
        request.setTitle("Rock Night");
        request.setStartsAt(startsAt);

        Map<String, Object> claims = Map.of(
                "id", organiserId.toString(),
                "email", "organiser@example.com"
        );

        when(eventRepository.existsByOrganiserIdAndVenue_IdAndTitleIgnoreCaseAndStartsAt(
                organiserId, venueId, "Rock Night", startsAt
        )).thenReturn(true);

        EventExistsException exception = assertThrows(
                EventExistsException.class,
                () -> organiserEventService.createEvent(request, claims)
        );

        assertEquals("Event already exists for this organiser, venue, title and start time", exception.getMessage());
    }

    @Test
    void updateEventThrowsWhenEndBeforeStart() {
        UUID eventId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);
        Event event = new Event();
        event.setStartsAt(start);
        event.setStatus(EventStatus.DRAFT);

        UpdateEventRequest request = new UpdateEventRequest();
        request.setEndsAt(start.minusSeconds(60));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organiserEventService.updateEvent(eventId, request)
        );

        assertEquals("endsAt must be greater than or equal to startsAt", exception.getMessage());
    }

    @Test
    void publishEventThrowsWhenNotDraft() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        InvalidEventStateException exception = assertThrows(
                InvalidEventStateException.class,
                () -> organiserEventService.publishEvent(eventId)
        );

        assertEquals("Only draft events can be published", exception.getMessage());
    }

    @Test
    void initializeInventoryReturnsZeroWhenAlreadyInitialized() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setStatus(EventStatus.DRAFT);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventSeatRepository.countByEvent_Id(eventId)).thenReturn(5L);

        long result = organiserEventService.initializeEventInventory(eventId);

        assertEquals(0L, result);
    }

    @Test
    void configurePricingThrowsOnDuplicateSectionIds() {
        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        Event event = new Event();
        event.setStatus(EventStatus.DRAFT);
        Venue venue = new Venue();
        venue.setId(venueId);
        event.setVenue(venue);

        VenueSection section = new VenueSection();
        section.setId(sectionId);
        section.setVenue(venue);

        EventPricingRequest.PriceItem p1 = new EventPricingRequest.PriceItem();
        p1.setSectionId(sectionId);
        p1.setPriceCents(1000);
        EventPricingRequest.PriceItem p2 = new EventPricingRequest.PriceItem();
        p2.setSectionId(sectionId);
        p2.setPriceCents(1200);

        EventPricingRequest request = new EventPricingRequest();
        request.setCurrency("inr");
        request.setPrices(List.of(p1, p2));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(venueSectionRepository.findByVenue_Id(venueId)).thenReturn(List.of(section));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organiserEventService.configureEventPricing(eventId, request)
        );

        assertEquals("Duplicate sectionId(s) in prices payload", exception.getMessage());
    }
}
