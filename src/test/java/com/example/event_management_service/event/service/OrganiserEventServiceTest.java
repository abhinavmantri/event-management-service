package com.example.event_management_service.event.service;

import com.example.event_management_service.event.dtos.CreateEventRequest;
import com.example.event_management_service.event.dtos.EventPricingRequest;
import com.example.event_management_service.event.dtos.UpdateEventRequest;
import com.example.event_management_service.event.exceptions.EventExistsException;
import com.example.event_management_service.event.exceptions.InvalidEventStateException;
import com.example.event_management_service.event.messaging.EventPublishedDomainEvent;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventSeat;
import com.example.event_management_service.event.model.EventSectionPricing;
import com.example.event_management_service.event.model.EventStatus;
import com.example.event_management_service.event.repository.EventRepository;
import com.example.event_management_service.event.repository.EventSeatRepository;
import com.example.event_management_service.event.repository.EventSectionPricingRepository;
import com.example.event_management_service.venue.model.Venue;
import com.example.event_management_service.venue.model.VenueSeat;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

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

        Event saved = organiserEventService.createEvent(request, organiserId, " organiser@example.com ");

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

        when(eventRepository.existsByOrganiserIdAndVenue_IdAndTitleIgnoreCaseAndStartsAt(
                organiserId, venueId, "Rock Night", startsAt
        )).thenReturn(true);

        EventExistsException exception = assertThrows(
                EventExistsException.class,
                () -> organiserEventService.createEvent(request, organiserId, "organiser@example.com")
        );

        assertEquals("Event already exists for this organiser, venue, title and start time", exception.getMessage());
    }

    @Test
    void updateEventThrowsWhenEndBeforeStart() {
        UUID eventId = UUID.randomUUID();
        UUID organiserId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);
        Event event = new Event();
        event.setId(eventId);
        event.setOrganiserId(organiserId);
        event.setStartsAt(start);
        event.setStatus(EventStatus.DRAFT);

        UpdateEventRequest request = new UpdateEventRequest();
        request.setEndsAt(start.minusSeconds(60));

        when(eventRepository.findByIdAndOrganiserId(eventId, organiserId)).thenReturn(Optional.of(event));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organiserEventService.updateEvent(eventId, request, organiserId)
        );

        assertEquals("endsAt must be greater than or equal to startsAt", exception.getMessage());
    }

    @Test
    void publishEventThrowsWhenNotDraft() {
        UUID eventId = UUID.randomUUID();
        UUID organiserId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        event.setOrganiserId(organiserId);
        event.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findByIdAndOrganiserId(eventId, organiserId)).thenReturn(Optional.of(event));

        InvalidEventStateException exception = assertThrows(
                InvalidEventStateException.class,
                () -> organiserEventService.publishEvent(eventId, organiserId)
        );

        assertEquals("Only draft events can be published", exception.getMessage());
    }

    @Test
    void publishEventThrowsWhenPricingMissing() {
        UUID eventId = UUID.randomUUID();
        UUID organiserId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        event.setOrganiserId(organiserId);
        event.setStatus(EventStatus.DRAFT);

        when(eventRepository.findByIdAndOrganiserId(eventId, organiserId)).thenReturn(Optional.of(event));
        when(eventSectionPricingRepository.findByEvent_Id(eventId)).thenReturn(List.of());

        InvalidEventStateException exception = assertThrows(
                InvalidEventStateException.class,
                () -> organiserEventService.publishEvent(eventId, organiserId)
        );

        assertEquals("Configure event pricing before publishing event", exception.getMessage());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void publishEventThrowsWhenInventoryMissing() {
        UUID eventId = UUID.randomUUID();
        UUID organiserId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        event.setOrganiserId(organiserId);
        event.setStatus(EventStatus.DRAFT);

        VenueSection section = new VenueSection();
        section.setId(UUID.randomUUID());
        section.setName("VIP");
        section.setSortOrder(1);
        EventSectionPricing pricing = EventSectionPricing.builder()
                .event(event)
                .section(section)
                .priceCents(5000)
                .currency("INR")
                .build();

        when(eventRepository.findByIdAndOrganiserId(eventId, organiserId)).thenReturn(Optional.of(event));
        when(eventSectionPricingRepository.findByEvent_Id(eventId)).thenReturn(List.of(pricing));
        when(eventSeatRepository.findByEvent_Id(eventId)).thenReturn(List.of());

        InvalidEventStateException exception = assertThrows(
                InvalidEventStateException.class,
                () -> organiserEventService.publishEvent(eventId, organiserId)
        );

        assertEquals("Initialize event inventory before publishing event", exception.getMessage());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void publishEventPersistsAndPublishesDomainEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID organiserId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        Venue venue = new Venue();
        venue.setId(venueId);

        Event event = new Event();
        event.setId(eventId);
        event.setVenue(venue);
        event.setOrganiserId(organiserId);
        event.setOrganiserEmail("org@example.com");
        event.setTitle("Rock Night");
        event.setCategory("Music");
        event.setStartsAt(Instant.parse("2026-04-01T18:00:00Z"));
        event.setEndsAt(Instant.parse("2026-04-01T21:00:00Z"));
        event.setStatus(EventStatus.DRAFT);

        VenueSection section = new VenueSection();
        section.setId(sectionId);
        section.setName("VIP");
        section.setSortOrder(1);

        VenueSeat venueSeat = VenueSeat.builder()
                .seatCode("VIP-R01-S01")
                .rowLabel("R01")
                .seatNumber(1)
                .section(section)
                .venue(venue)
                .createdAt(Instant.now())
                .build();
        venueSeat.setId(UUID.randomUUID());

        EventSectionPricing pricing = EventSectionPricing.builder()
                .event(event)
                .section(section)
                .priceCents(5000)
                .currency("INR")
                .build();
        EventSeat eventSeat = EventSeat.builder()
                .event(event)
                .venueSeat(venueSeat)
                .section(section)
                .priceCents(5000)
                .currency("INR")
                .status(com.example.event_management_service.event.model.EventSeatStatus.AVAILABLE)
                .version(0)
                .createdAt(Instant.now())
                .build();
        eventSeat.setId(UUID.randomUUID());

        when(eventRepository.findByIdAndOrganiserId(eventId, organiserId)).thenReturn(Optional.of(event));
        when(eventSectionPricingRepository.findByEvent_Id(eventId)).thenReturn(List.of(pricing));
        when(eventSeatRepository.findByEvent_Id(eventId)).thenReturn(List.of(eventSeat));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event saved = organiserEventService.publishEvent(eventId, organiserId);

        assertSame(event, saved);
        assertEquals(EventStatus.PUBLISHED, saved.getStatus());
        ArgumentCaptor<Object> domainEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(domainEventCaptor.capture());

        EventPublishedDomainEvent domainEvent = (EventPublishedDomainEvent) domainEventCaptor.getValue();
        assertEquals(eventId, domainEvent.eventId());
        assertEquals(venueId, domainEvent.venueId());
        assertEquals(organiserId, domainEvent.organiserId());
        assertEquals(1, domainEvent.sectionPrices().size());
        assertEquals(sectionId, domainEvent.sectionPrices().getFirst().sectionId());
        assertEquals(5000, domainEvent.sectionPrices().getFirst().priceCents());
        assertEquals("INR", domainEvent.sectionPrices().getFirst().currency());
        assertEquals(1, domainEvent.seats().size());
        assertEquals(eventSeat.getId(), domainEvent.seats().getFirst().eventSeatId());
        assertEquals(venueSeat.getId(), domainEvent.seats().getFirst().venueSeatId());
        assertEquals(sectionId, domainEvent.seats().getFirst().sectionId());
    }

    @Test
    void initializeInventoryReturnsZeroWhenAlreadyInitialized() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID organiserId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        event.setOrganiserId(organiserId);
        event.setStatus(EventStatus.DRAFT);

        when(eventRepository.findByIdAndOrganiserId(eventId, organiserId)).thenReturn(Optional.of(event));
        when(eventSeatRepository.countByEvent_Id(eventId)).thenReturn(5L);

        long result = organiserEventService.initializeEventInventory(eventId, organiserId);

        assertEquals(0L, result);
    }

    @Test
    void configurePricingThrowsOnDuplicateSectionIds() {
        UUID eventId = UUID.randomUUID();
        UUID organiserId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);
        event.setOrganiserId(organiserId);
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

        when(eventRepository.findByIdAndOrganiserId(eventId, organiserId)).thenReturn(Optional.of(event));
        when(venueSectionRepository.findByVenue_Id(venueId)).thenReturn(List.of(section));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> organiserEventService.configureEventPricing(eventId, request, organiserId)
        );

        assertEquals("Duplicate sectionId(s) in prices payload", exception.getMessage());
    }
}
