package com.example.event_management_service.event.service;

import com.example.event_management_service.event.dtos.CreateEventRequest;
import com.example.event_management_service.event.dtos.EventPricingRequest;
import com.example.event_management_service.event.dtos.UpdateEventRequest;
import com.example.event_management_service.event.exceptions.EventExistsException;
import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.exceptions.InvalidEventStateException;
import com.example.event_management_service.event.messaging.EventPublishedDomainEvent;
import com.example.event_management_service.event.messaging.EventPublishedKafkaMessage;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventSeat;
import com.example.event_management_service.event.model.EventSeatStatus;
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class OrganiserEventService {
    private static final String LOG_GROUP_CREATE = "[ORGANISER_EVENT_SERVICE][CREATE_EVENT]";
    private static final String LOG_GROUP_UPDATE = "[ORGANISER_EVENT_SERVICE][UPDATE_EVENT]";
    private static final String LOG_GROUP_PUBLISH = "[ORGANISER_EVENT_SERVICE][PUBLISH_EVENT]";
    private static final String LOG_GROUP_INVENTORY = "[ORGANISER_EVENT_SERVICE][INITIALIZE_INVENTORY]";
    private static final String LOG_GROUP_PRICING = "[ORGANISER_EVENT_SERVICE][CONFIGURE_PRICING]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private final EventRepository eventRepository;
    private final EventSeatRepository eventSeatRepository;
    private final EventSectionPricingRepository eventSectionPricingRepository;
    private final VenueSeatRepository venueSeatRepository;
    private final VenueSectionRepository venueSectionRepository;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public OrganiserEventService(
            EventRepository eventRepository,
            EventSeatRepository eventSeatRepository,
            EventSectionPricingRepository eventSectionPricingRepository,
            VenueSeatRepository venueSeatRepository,
            VenueSectionRepository venueSectionRepository,
            EntityManager entityManager,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.eventRepository = eventRepository;
        this.eventSeatRepository = eventSeatRepository;
        this.eventSectionPricingRepository = eventSectionPricingRepository;
        this.venueSeatRepository = venueSeatRepository;
        this.venueSectionRepository = venueSectionRepository;
        this.entityManager = entityManager;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Event createEvent(CreateEventRequest request, Map<String, Object> claims) throws EventExistsException {
        String requestId = requestId();
        log.info("{} request: requestId={}, venueId={}, title={}, startsAt={}", LOG_GROUP_CREATE, requestId, request.getVenueId(), request.getTitle(), request.getStartsAt());
        Instant now = Instant.now();
        String organiserId = getRequiredClaimAsText(claims, "id");
        String organiserEmail = getRequiredClaimAsText(claims, "email");
        UUID organiserUuid = UUID.fromString(organiserId);
        String normalizedTitle = request.getTitle().trim();

        boolean exists = eventRepository.existsByOrganiserIdAndVenue_IdAndTitleIgnoreCaseAndStartsAt(
            organiserUuid,
            request.getVenueId(),
            normalizedTitle,
            request.getStartsAt()
        );
        if (exists) {
            log.warn("{} failure: requestId={}, reason=Event already exists for organiser/venue/title/start", LOG_GROUP_CREATE, requestId);
            throw new EventExistsException("Event already exists for this organiser, venue, title and start time");
        }

        Event event = Event.builder()
                .organiserId(organiserUuid)
                .organiserEmail(normalizeOptionalText(organiserEmail))
                .venue(entityManager.getReference(Venue.class, request.getVenueId()))
                .title(normalizedTitle)
                .description(normalizeOptionalText(request.getDescription()))
                .category(normalizeOptionalText(request.getCategory()))
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .status(EventStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("{} success: requestId={}, eventId={}", LOG_GROUP_CREATE, requestId, savedEvent.getId());
        return savedEvent;
    }

    @Transactional
    public Event updateEvent(UUID eventId, UpdateEventRequest request) throws EventNotFoundException {
        String requestId = requestId();
        log.info("{} request: requestId={}, eventId={}", LOG_GROUP_UPDATE, requestId, eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found"));

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            event.setDescription(normalizeOptionalText(request.getDescription()));
        }

        if (request.getCategory() != null) {
            event.setCategory(normalizeOptionalText(request.getCategory()));
        }

        if (request.getStartsAt() != null) {
            event.setStartsAt(request.getStartsAt());
        }

        if (request.getEndsAt() != null) {
            event.setEndsAt(request.getEndsAt());
        }

        if (event.getEndsAt() != null && event.getEndsAt().isBefore(event.getStartsAt())) {
            log.warn("{} failure: requestId={}, eventId={}, reason=endsAt before startsAt", LOG_GROUP_UPDATE, requestId, eventId);
            throw new IllegalArgumentException("endsAt must be greater than or equal to startsAt");
        }

        event.setUpdatedAt(Instant.now());
        Event savedEvent = eventRepository.save(event);
        log.info("{} success: requestId={}, eventId={}", LOG_GROUP_UPDATE, requestId, eventId);
        return savedEvent;
    }

    @Transactional
    public Event publishEvent(UUID eventId) throws EventNotFoundException, InvalidEventStateException {
        String requestId = requestId();
        log.info("{} request: requestId={}, eventId={}", LOG_GROUP_PUBLISH, requestId, eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found"));

        if (event.getStatus() != EventStatus.DRAFT) {
            log.warn("{} failure: requestId={}, eventId={}, status={}", LOG_GROUP_PUBLISH, requestId, eventId, event.getStatus());
            throw new InvalidEventStateException("Only draft events can be published");
        }

        List<EventSectionPricing> pricingList = eventSectionPricingRepository.findByEvent_Id(eventId);
        if (pricingList.isEmpty()) {
            log.warn("{} failure: requestId={}, eventId={}, reason=missing pricing", LOG_GROUP_PUBLISH, requestId, eventId);
            throw new InvalidEventStateException("Configure event pricing before publishing event");
        }

        Instant publishedAt = Instant.now();
        event.setStatus(EventStatus.PUBLISHED);
        event.setUpdatedAt(publishedAt);
        Event savedEvent = eventRepository.save(event);
        applicationEventPublisher.publishEvent(buildEventPublishedDomainEvent(savedEvent, pricingList, publishedAt));
        log.info("{} success: requestId={}, eventId={}", LOG_GROUP_PUBLISH, requestId, eventId);
        return savedEvent;
    }

    @Transactional
    public long initializeEventInventory(UUID eventId) throws EventNotFoundException, InvalidEventStateException {
        String requestId = requestId();
        log.info("{} request: requestId={}, eventId={}", LOG_GROUP_INVENTORY, requestId, eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found"));

        if (event.getStatus() != EventStatus.DRAFT) {
            log.warn("{} failure: requestId={}, eventId={}, status={}", LOG_GROUP_INVENTORY, requestId, eventId, event.getStatus());
            throw new InvalidEventStateException("Inventory can be initialized only for draft or published events");
        }

        if (eventSeatRepository.countByEvent_Id(eventId) > 0) {
            log.info("{} success: requestId={}, eventId={}, createdSeats=0, alreadyInitialized=true", LOG_GROUP_INVENTORY, requestId, eventId);
            return 0L;
        }

        List<EventSectionPricing> pricingList = eventSectionPricingRepository.findByEvent_Id(eventId);
        if (pricingList.isEmpty()) {
            log.warn("{} failure: requestId={}, eventId={}, reason=missing pricing", LOG_GROUP_INVENTORY, requestId, eventId);
            throw new InvalidEventStateException("Configure event pricing before initializing inventory");
        }

        Map<UUID, EventSectionPricing> pricingBySectionId = new HashMap<>(pricingList.size());
        for (EventSectionPricing pricing : pricingList) {
            pricingBySectionId.put(pricing.getSection().getId(), pricing);
        }

        List<VenueSeat> venueSeats = venueSeatRepository.findByVenue_Id(event.getVenue().getId());
        if (venueSeats.isEmpty()) {
            log.info("{} success: requestId={}, eventId={}, createdSeats=0, reason=no venue seats", LOG_GROUP_INVENTORY, requestId, eventId);
            return 0L;
        }

        Instant now = Instant.now();
        List<EventSeat> eventSeats = new ArrayList<>(venueSeats.size());
        for (VenueSeat venueSeat : venueSeats) {
            EventSectionPricing pricing = pricingBySectionId.get(venueSeat.getSection().getId());
            if (pricing == null) {
                log.warn("{} failure: requestId={}, eventId={}, reason=missing pricing for section {}", LOG_GROUP_INVENTORY, requestId, eventId, venueSeat.getSection().getId());
                throw new InvalidEventStateException("Missing pricing for section: " + venueSeat.getSection().getId());
            }

            eventSeats.add(EventSeat.builder()
                    .event(event)
                    .venueSeat(venueSeat)
                    .section(venueSeat.getSection())
                    .priceCents(pricing.getPriceCents())
                    .currency(pricing.getCurrency())
                    .status(EventSeatStatus.AVAILABLE)
                    .createdAt(now)
                    .version(0)
                    .build());
        }

        eventSeatRepository.saveAll(eventSeats);
        log.info("{} success: requestId={}, eventId={}, createdSeats={}", LOG_GROUP_INVENTORY, requestId, eventId, eventSeats.size());
        return eventSeats.size();
    }

    @Transactional
    public List<EventSectionPricing> configureEventPricing(UUID eventId, EventPricingRequest request)
            throws EventNotFoundException, InvalidEventStateException {
        String requestId = requestId();
        int priceItemsCount = request.getPrices() == null ? 0 : request.getPrices().size();
        log.info("{} request: requestId={}, eventId={}, currency={}, priceItemsCount={}", LOG_GROUP_PRICING, requestId, eventId, request.getCurrency(), priceItemsCount);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found"));

        if (event.getStatus() != EventStatus.DRAFT) {
            log.warn("{} failure: requestId={}, eventId={}, status={}", LOG_GROUP_PRICING, requestId, eventId, event.getStatus());
            throw new InvalidEventStateException("Pricing can be configured only for draft events");
        }

        String currency = normalizeCurrency(request.getCurrency());
        if (request.getPrices() == null || request.getPrices().isEmpty()) {
            throw new IllegalArgumentException("prices must not be empty");
        }

        List<VenueSection> sections = venueSectionRepository.findByVenue_Id(event.getVenue().getId());
        Map<UUID, VenueSection> sectionById = new HashMap<>();
        for (VenueSection section : sections) {
            sectionById.put(section.getId(), section);
        }

        Set<UUID> duplicateSectionIds = new HashSet<>();
        Set<UUID> seenSectionIds = new HashSet<>();
        List<String> unknownSectionIds = new ArrayList<>();
        for (EventPricingRequest.PriceItem priceItem : request.getPrices()) {
            UUID sectionId = priceItem.getSectionId();
            if (!seenSectionIds.add(sectionId)) {
                duplicateSectionIds.add(sectionId);
            }
            if (!sectionById.containsKey(sectionId)) {
                unknownSectionIds.add(sectionId.toString());
            }
        }
        if (!duplicateSectionIds.isEmpty()) {
            log.warn("{} failure: requestId={}, eventId={}, reason=duplicate section ids", LOG_GROUP_PRICING, requestId, eventId);
            throw new IllegalArgumentException("Duplicate sectionId(s) in prices payload");
        }
        if (!unknownSectionIds.isEmpty()) {
            log.warn("{} failure: requestId={}, eventId={}, reason=unknown section ids", LOG_GROUP_PRICING, requestId, eventId);
            throw new IllegalArgumentException("Section(s) do not belong to the event venue: " + String.join(", ", unknownSectionIds));
        }

        List<EventSectionPricing> existingPricing = eventSectionPricingRepository.findByEvent_Id(eventId);
        Map<UUID, EventSectionPricing> pricingBySectionId = new HashMap<>();
        for (EventSectionPricing pricing : existingPricing) {
            pricingBySectionId.put(pricing.getSection().getId(), pricing);
        }

        List<EventSectionPricing> toSave = new ArrayList<>();
        for (EventPricingRequest.PriceItem priceItem : request.getPrices()) {
            UUID sectionId = priceItem.getSectionId();
            VenueSection section = sectionById.get(sectionId);
            EventSectionPricing pricing = pricingBySectionId.get(section.getId());
            if (pricing == null) {
                pricing = EventSectionPricing.builder()
                        .event(event)
                        .section(section)
                        .build();
            }
            pricing.setPriceCents(requirePositivePrice(priceItem.getPriceCents(), "priceCents"));
            pricing.setCurrency(currency);
            toSave.add(pricing);
        }

        List<EventSectionPricing> savedPricing = eventSectionPricingRepository.saveAll(toSave);
        log.info("{} success: requestId={}, eventId={}, savedPricings={}", LOG_GROUP_PRICING, requestId, eventId, savedPricing.size());
        return savedPricing;
    }

    private String getRequiredClaimAsText(Map<String, Object> claims, String claimName) {
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

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String valueText = value.trim();
        return valueText.isEmpty() ? null : valueText;
    }

    private Integer requirePositivePrice(Integer priceCents, String fieldName) {
        if (priceCents == null || priceCents <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return priceCents;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            return "INR";
        }
        String normalized = currency.trim().toUpperCase();
        if (normalized.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code");
        }
        return normalized;
    }

    private String requestId() {
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        return requestId == null ? "N/A" : requestId;
    }

    private EventPublishedDomainEvent buildEventPublishedDomainEvent(
            Event event,
            List<EventSectionPricing> pricingList,
            Instant publishedAt
    ) {
        List<EventPublishedKafkaMessage.SectionPrice> sectionPrices = pricingList.stream()
                .map(pricing -> new EventPublishedKafkaMessage.SectionPrice(
                        pricing.getSection().getId(),
                        pricing.getSection().getName(),
                        pricing.getSection().getSortOrder(),
                        pricing.getPriceCents(),
                        pricing.getCurrency()
                ))
                .toList();

        return new EventPublishedDomainEvent(
                event.getId(),
                event.getVenue().getId(),
                event.getOrganiserId(),
                event.getOrganiserEmail(),
                event.getTitle(),
                event.getCategory(),
                event.getStartsAt(),
                event.getEndsAt(),
                publishedAt,
                sectionPrices
        );
    }
}
