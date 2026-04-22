package com.example.event_management_service.venue.controller;

import com.example.event_management_service.venue.dtos.CreateVenueSectionRequest;
import com.example.event_management_service.venue.dtos.CreateVenueSectionResponse;
import com.example.event_management_service.venue.dtos.GenerateSeatsRequest;
import com.example.event_management_service.venue.dtos.SectionSeatsResponse;
import com.example.event_management_service.shared.model.ApiResponse;
import com.example.event_management_service.shared.model.ResponseStatus;
import com.example.event_management_service.venue.exceptions.InvalidVenueStateException;
import com.example.event_management_service.venue.exceptions.SeatsAlreadyGeneratedException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.exceptions.VenueSectionExistsException;
import com.example.event_management_service.venue.model.VenueSection;
import com.example.event_management_service.venue.service.VenueSectionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/venues/{venueId}/sections")
@Slf4j
public class VenueSectionController {
    private static final String LOG_GROUP_CREATE_SECTION = "[VENUE_SECTION_CONTROLLER][CREATE_SECTION]";
    private static final String LOG_GROUP_GENERATE_SEATS = "[VENUE_SECTION_CONTROLLER][GENERATE_SEATS]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final VenueSectionService venueSectionService;

    @Autowired
    public VenueSectionController(VenueSectionService venueSectionService) {
        this.venueSectionService = venueSectionService;
    }

    @PostMapping
    public ResponseEntity<CreateVenueSectionResponse> createVenueSection(
            @PathVariable UUID venueId,
            @RequestBody @Valid CreateVenueSectionRequest request
            ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        log.info("{} request: requestId={}, venueId={}", LOG_GROUP_CREATE_SECTION, requestId, venueId);
        CreateVenueSectionResponse response = new CreateVenueSectionResponse();
        try {
            VenueSection section = venueSectionService.createVenueSection(
                    venueId,
                    request.getName(),
                    request.getSortOrder()
            );
            response.setVenueSection(section);
            response.setResponseStatus(ResponseStatus.SUCCESS);
            response.setMessage("Section created successfully");
            log.info("{} success: requestId={}, venueId={}, sectionId={}, latencyMs={}", LOG_GROUP_CREATE_SECTION, requestId, venueId, section.getId(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (VenueNotFoundException ex) {
            setFailureResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, venueId={}, reason={}, latencyMs={}", LOG_GROUP_CREATE_SECTION, requestId, venueId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (VenueSectionExistsException ex) {
            setFailureResponse(response, ex.getMessage());
            log.warn("{} failure: requestId={}, venueId={}, reason={}, latencyMs={}", LOG_GROUP_CREATE_SECTION, requestId, venueId, ex.getMessage(), elapsedMillis(startNanos));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    @PostMapping("/{sectionId}/seats/generate")
    public ResponseEntity<SectionSeatsResponse> generateSectionSeats(
            @PathVariable UUID venueId,
            @PathVariable UUID sectionId,
            @RequestBody @Valid GenerateSeatsRequest request
    ) {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        long startNanos = System.nanoTime();
        int totalSeats = request.getRowCount() * request.getSeatsPerRow();
        log.info(
                "{} request: requestId={}, venueId={}, sectionId={}, rowCount={}, seatsPerRow={}, rowLabelType={}, startSeatNumber={}, totalSeats={}",
                LOG_GROUP_GENERATE_SEATS,
                requestId,
                venueId,
                sectionId,
                request.getRowCount(),
                request.getSeatsPerRow(),
                request.getRowLabelType(),
                request.getStartSeatNumber(),
                totalSeats
        );
        SectionSeatsResponse response = new SectionSeatsResponse();
        try {
            response.setSeats(venueSectionService.generateSectionSeats(venueId, sectionId, request));
            response.setResponseStatus(ResponseStatus.SUCCESS);
            response.setMessage("Section seats generated successfully");
            log.info(
                    "{} success: requestId={}, venueId={}, sectionId={}, latencyMs={}",
                    LOG_GROUP_GENERATE_SEATS, requestId, venueId, sectionId, elapsedMillis(startNanos)
            );
            return ResponseEntity.ok(response);
        } catch (VenueNotFoundException ex) {
            setFailureResponse(response, ex.getMessage());
            log.warn(
                    "{} failure: requestId={}, venueId={}, sectionId={}, reason={}, latencyMs={}",
                    LOG_GROUP_GENERATE_SEATS, requestId, venueId, sectionId, ex.getMessage(), elapsedMillis(startNanos)
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (InvalidVenueStateException | SeatsAlreadyGeneratedException ex) {
            setFailureResponse(response, ex.getMessage());
            log.warn(
                    "{} failure: requestId={}, venueId={}, sectionId={}, reason={}, latencyMs={}",
                    LOG_GROUP_GENERATE_SEATS, requestId, venueId, sectionId, ex.getMessage(), elapsedMillis(startNanos)
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private <T extends ApiResponse> void setFailureResponse(T response, String message) {
        response.setResponseStatus(ResponseStatus.FAILURE);
        response.setMessage(message);
    }
}
