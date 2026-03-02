package com.example.event_management_service.venue.service;

import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventStatus;
import com.example.event_management_service.event.repository.EventRepository;
import com.example.event_management_service.venue.dtos.GenerateSeatsRequest;
import com.example.event_management_service.venue.exceptions.InvalidVenueStateException;
import com.example.event_management_service.venue.exceptions.SeatsAlreadyGeneratedException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.exceptions.VenueSectionExistsException;
import com.example.event_management_service.venue.model.Venue;
import com.example.event_management_service.venue.model.VenueSeat;
import com.example.event_management_service.venue.model.VenueSection;
import com.example.event_management_service.venue.repository.VenueRepository;
import com.example.event_management_service.venue.repository.VenueSeatRepository;
import com.example.event_management_service.venue.repository.VenueSectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class VenueSectionService {
    private static final String LOG_GROUP_CREATE_SECTION = "[VENUE_SECTION_SERVICE][CREATE_SECTION]";
    private static final String LOG_GROUP_GENERATE_SEATS = "[VENUE_SECTION_SERVICE][GENERATE_SEATS]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final int ROW_LABEL_TYPE_NUMERIC = 0;
    private static final int ROW_LABEL_TYPE_ALPHA = 1;

    private final VenueSectionRepository venueSectionRepository;
    private final VenueSeatRepository venueSeatRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;

    @Autowired
    public VenueSectionService(VenueSectionRepository venueSectionRepository,
                               VenueSeatRepository venueSeatRepository,
                               VenueRepository venueRepository,
                               EventRepository eventRepository) {
        this.venueSectionRepository = venueSectionRepository;
        this.venueSeatRepository = venueSeatRepository;
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
    }

    public VenueSection createVenueSection(UUID venueId, String name, int sortOrder) throws VenueNotFoundException, VenueSectionExistsException {
        long startNanos = System.nanoTime();
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        String normalizedName = name == null ? null : name.trim();
        log.info("{} request: requestId={}, venueId={}, name={}, sortOrder={}", LOG_GROUP_CREATE_SECTION, requestId, venueId, normalizedName, sortOrder);
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new VenueNotFoundException("Venue not found"));
        boolean sectionExists = venueSectionRepository.existsByVenue_IdAndNameIgnoreCase(venueId, normalizedName);
        if (sectionExists) {
            log.warn("{} failure: requestId={}, venueId={}, reason=section already exists, latencyMs={}", LOG_GROUP_CREATE_SECTION, requestId, venueId, elapsedMillis(startNanos));
            throw new VenueSectionExistsException("Section already exists for this venue");
        }

        VenueSection section = VenueSection.builder()
                .venue(venue)
                .name(normalizedName)
                .sortOrder(sortOrder)
                .createdAt(Instant.now())
                .build();
        VenueSection savedSection = venueSectionRepository.save(section);
        log.info("{} success: requestId={}, venueId={}, sectionId={}, latencyMs={}", LOG_GROUP_CREATE_SECTION, requestId, venueId, savedSection.getId(), elapsedMillis(startNanos));
        return savedSection;
    }

    @Transactional
    public List<VenueSeat> generateSectionSeats(UUID venueId, UUID sectionId, GenerateSeatsRequest request) throws VenueNotFoundException, InvalidVenueStateException, SeatsAlreadyGeneratedException {
        long startNanos = System.nanoTime();
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        log.info("{} request: requestId={}, venueId={}, sectionId={}, rowCount={}, seatsPerRow={}, rowLabelType={}", LOG_GROUP_GENERATE_SEATS, requestId, venueId, sectionId, request.getRowCount(), request.getSeatsPerRow(), request.getRowLabelType());
        VenueSection section = venueSectionRepository.findByIdAndVenue_Id(sectionId, venueId)
                .orElseThrow(() -> new VenueNotFoundException("Section not found for venue"));

        boolean venueUsedByPublishedEvent = eventRepository.findAll().stream()
                .anyMatch(event -> isPublishedEventForVenue(event, venueId));
        if (venueUsedByPublishedEvent) {
            log.warn("{} failure: requestId={}, venueId={}, sectionId={}, reason=venue used by published event, latencyMs={}", LOG_GROUP_GENERATE_SEATS, requestId, venueId, sectionId, elapsedMillis(startNanos));
            throw new InvalidVenueStateException("Cannot generate seats: venue is used by a published event");
        }

        long existingSeats = venueSeatRepository.countByVenue_IdAndSection_Id(venueId, sectionId);
        if (existingSeats > 0) {
            log.warn("{} failure: requestId={}, venueId={}, sectionId={}, reason=seats already exist, existingSeats={}, latencyMs={}", LOG_GROUP_GENERATE_SEATS, requestId, venueId, sectionId, existingSeats, elapsedMillis(startNanos));
            throw new SeatsAlreadyGeneratedException("Seats already exist for this section");
        }

        int totalSeats = request.getRowCount() * request.getSeatsPerRow();
        List<VenueSeat> newSeats = new ArrayList<>(totalSeats);
        Instant now = Instant.now();
        String sectionToken = normalizeSectionToken(section.getName());
        int rowWidth = Math.max(2, String.valueOf(request.getRowCount()).length());
        int seatWidth = Math.max(2, String.valueOf(request.getSeatsPerRow()).length());
        boolean alphaRowLabels = request.getRowLabelType() == ROW_LABEL_TYPE_ALPHA;

        for (int rowIndex = 1; rowIndex <= request.getRowCount(); rowIndex++) {
            String rowLabel = alphaRowLabels
                    ? toAlphabeticRowLabel(rowIndex)
                    : "R" + padNumber(rowIndex, rowWidth);
            for (int seatOffset = 0; seatOffset < request.getSeatsPerRow(); seatOffset++) {
                int seatNumber = seatOffset + 1;
                String seatCode = sectionToken + "-" + rowLabel + "-S" + padNumber(seatNumber, seatWidth);
                newSeats.add(VenueSeat.builder()
                        .venue(section.getVenue())
                        .section(section)
                        .seatCode(seatCode)
                        .rowLabel(rowLabel)
                        .seatNumber(seatNumber)
                        .createdAt(now)
                        .build());
            }
        }

        List<VenueSeat> savedSeats = venueSeatRepository.saveAll(newSeats);
        log.info("{} success: requestId={}, venueId={}, sectionId={}, generatedSeats={}, latencyMs={}", LOG_GROUP_GENERATE_SEATS, requestId, venueId, sectionId, savedSeats.size(), elapsedMillis(startNanos));
        return savedSeats;
    }

    private boolean isPublishedEventForVenue(Event event, UUID venueId) {
        return event.getStatus() == EventStatus.PUBLISHED
                && event.getVenue() != null
                && venueId.equals(event.getVenue().getId());
    }

    private String normalizeSectionToken(String sectionName) {
        String normalized = sectionName == null ? "" : sectionName.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "-");
        normalized = normalized.replaceAll("^-+|-+$", "");
        return normalized.isEmpty() ? "SECTION" : normalized;
    }

    private String padNumber(int value, int width) {
        return String.format("%0" + width + "d", value);
    }

    private String toAlphabeticRowLabel(int rowIndex) {
        StringBuilder label = new StringBuilder();
        int value = rowIndex;
        while (value > 0) {
            value--;
            label.insert(0, (char) ('A' + (value % 26)));
            value /= 26;
        }
        return label.toString();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
