package com.example.event_management_service.venue.service;

import com.example.event_management_service.venue.exceptions.VenueExistsException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.model.Venue;
import com.example.event_management_service.venue.repository.VenueRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class VenueService {
    private static final String LOG_GROUP_CREATE = "[VENUE_SERVICE][CREATE_VENUE]";
    private static final String LOG_GROUP_LIST = "[VENUE_SERVICE][LIST_VENUES]";
    private static final String LOG_GROUP_SEARCH = "[VENUE_SERVICE][SEARCH_VENUES]";
    private static final String LOG_GROUP_GET_BY_ID = "[VENUE_SERVICE][GET_VENUE_BY_ID]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final VenueRepository venueRepository;

    @Autowired
    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public Venue createVenue(String name, String city, String address) throws VenueExistsException {
        String requestId = requestId();
        log.info("{} request: requestId={}, name={}, city={}", LOG_GROUP_CREATE, requestId, name, city);
        String normalizedName = name.trim();
        String normalizedCity = city.trim();
        String normalizedAddress = address.trim();

        boolean venueExists = venueRepository.existsByNameIgnoreCaseAndCityIgnoreCase(
                normalizedName,
                normalizedCity
        );
        if (venueExists) {
            log.warn("{} failure: requestId={}, reason=venue_already_exists", LOG_GROUP_CREATE, requestId);
            throw new VenueExistsException("Venue already exists for this name, city and address");
        }

        Instant now = Instant.now();
        Venue venue = Venue.builder()
                .name(normalizedName)
                .city(normalizedCity)
                .address(normalizedAddress)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Venue savedVenue = venueRepository.save(venue);
        log.info("{} success: requestId={}, venueId={}", LOG_GROUP_CREATE, requestId, savedVenue.getId());
        return savedVenue;
    }

    public List<Venue> listVenues() {
        String requestId = requestId();
        log.info("{} request: requestId={}", LOG_GROUP_LIST, requestId);
        List<Venue> venues = venueRepository.findAll();
        log.info("{} success: requestId={}, count={}", LOG_GROUP_LIST, requestId, venues.size());
        return venues;
    }

    public Page<Venue> searchVenues(String query, Pageable pageable) {
        String requestId = requestId();
        log.info(
                "{} request: requestId={}, query={}, page={}, size={}",
                LOG_GROUP_SEARCH, requestId, query, pageable.getPageNumber(), pageable.getPageSize()
        );
        Pageable effectivePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        String normalizedQuery = normalizeText(query);
        Page<Venue> result = venueRepository.searchVenues(normalizedQuery, effectivePageable);
        log.info(
                "{} success: requestId={}, resultSize={}, totalElements={}",
                LOG_GROUP_SEARCH, requestId, result.getNumberOfElements(), result.getTotalElements()
        );
        return result;
    }

    public Venue getVenueById(UUID venueId) throws VenueNotFoundException {
        String requestId = requestId();
        log.info("{} request: requestId={}, venueId={}", LOG_GROUP_GET_BY_ID, requestId, venueId);
        return venueRepository.findById(venueId)
                .map(venue -> {
                    log.info("{} success: requestId={}, venueId={}", LOG_GROUP_GET_BY_ID, requestId, venueId);
                    return venue;
                })
                .orElseThrow(() -> {
                    log.warn("{} failure: requestId={}, venueId={}, reason=venue_not_found", LOG_GROUP_GET_BY_ID, requestId, venueId);
                    return new VenueNotFoundException("Venue not found");
                });
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
