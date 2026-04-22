package com.example.event_management_service.venue.controller;

import com.example.event_management_service.shared.model.ResponseStatus;
import com.example.event_management_service.venue.dtos.CreateVenueSectionRequest;
import com.example.event_management_service.venue.dtos.CreateVenueSectionResponse;
import com.example.event_management_service.venue.dtos.GenerateSeatsRequest;
import com.example.event_management_service.venue.dtos.SectionSeatsResponse;
import com.example.event_management_service.venue.exceptions.InvalidVenueStateException;
import com.example.event_management_service.venue.exceptions.SeatsAlreadyGeneratedException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.exceptions.VenueSectionExistsException;
import com.example.event_management_service.venue.model.VenueSeat;
import com.example.event_management_service.venue.model.VenueSection;
import com.example.event_management_service.venue.service.VenueSectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueSectionControllerTest {

    @Mock
    private VenueSectionService venueSectionService;

    @InjectMocks
    private VenueSectionController venueSectionController;

    @Test
    void createVenueSectionSuccessReturnsCreatedResponse() throws VenueNotFoundException, VenueSectionExistsException {
        UUID venueId = UUID.randomUUID();
        CreateVenueSectionRequest request = new CreateVenueSectionRequest();
        request.setName("VIP");
        request.setSortOrder(1);

        VenueSection section = new VenueSection();
        section.setId(UUID.randomUUID());
        when(venueSectionService.createVenueSection(venueId, "VIP", 1)).thenReturn(section);

        ResponseEntity<CreateVenueSectionResponse> responseEntity = venueSectionController.createVenueSection(venueId, request);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        CreateVenueSectionResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.SUCCESS, response.getResponseStatus());
        assertEquals("Section created successfully", response.getMessage());
        assertSame(section, response.getVenueSection());
        verify(venueSectionService).createVenueSection(venueId, "VIP", 1);
    }

    @Test
    void generateSectionSeatsSuccessReturnsOkResponse() throws VenueNotFoundException, InvalidVenueStateException, SeatsAlreadyGeneratedException {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        GenerateSeatsRequest request = new GenerateSeatsRequest();
        request.setRowCount(2);
        request.setSeatsPerRow(2);
        request.setRowLabelType(0);
        request.setStartSeatNumber(1);

        VenueSeat seat = new VenueSeat();
        seat.setSeatCode("VIP-R01-S01");
        List<VenueSeat> seats = List.of(seat);
        when(venueSectionService.generateSectionSeats(venueId, sectionId, request)).thenReturn(seats);

        ResponseEntity<SectionSeatsResponse> responseEntity = venueSectionController.generateSectionSeats(venueId, sectionId, request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        SectionSeatsResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.SUCCESS, response.getResponseStatus());
        assertEquals("Section seats generated successfully", response.getMessage());
        assertSame(seats, response.getSeats());
        verify(venueSectionService).generateSectionSeats(venueId, sectionId, request);
    }

    @Test
    void generateSectionSeatsAlphaRowTypeSuccessReturnsOkResponse() throws VenueNotFoundException, InvalidVenueStateException, SeatsAlreadyGeneratedException {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        GenerateSeatsRequest request = new GenerateSeatsRequest();
        request.setRowCount(2);
        request.setSeatsPerRow(1);
        request.setRowLabelType(1);
        request.setStartSeatNumber(1);

        VenueSeat seat = new VenueSeat();
        seat.setSeatCode("VIP-A-S01");
        seat.setRowLabel("A");
        seat.setSeatNumber(1);
        List<VenueSeat> seats = List.of(seat);
        when(venueSectionService.generateSectionSeats(venueId, sectionId, request)).thenReturn(seats);

        ResponseEntity<SectionSeatsResponse> responseEntity = venueSectionController.generateSectionSeats(venueId, sectionId, request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        SectionSeatsResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.SUCCESS, response.getResponseStatus());
        assertEquals("Section seats generated successfully", response.getMessage());
        assertSame(seats, response.getSeats());
        verify(venueSectionService).generateSectionSeats(venueId, sectionId, request);
    }

    @Test
    void generateSectionSeatsSectionMissingReturnsNotFound() throws VenueNotFoundException, InvalidVenueStateException, SeatsAlreadyGeneratedException {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        GenerateSeatsRequest request = new GenerateSeatsRequest();
        request.setRowCount(1);
        request.setSeatsPerRow(1);
        request.setRowLabelType(0);
        request.setStartSeatNumber(1);
        when(venueSectionService.generateSectionSeats(venueId, sectionId, request))
                .thenThrow(new VenueNotFoundException("Section not found for venue"));

        ResponseEntity<SectionSeatsResponse> responseEntity = venueSectionController.generateSectionSeats(venueId, sectionId, request);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        SectionSeatsResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.FAILURE, response.getResponseStatus());
        assertEquals("Section not found for venue", response.getMessage());
    }

    @Test
    void generateSectionSeatsConflictReturnsConflictResponse() throws VenueNotFoundException, InvalidVenueStateException, SeatsAlreadyGeneratedException {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        GenerateSeatsRequest request = new GenerateSeatsRequest();
        request.setRowCount(1);
        request.setSeatsPerRow(1);
        request.setRowLabelType(0);
        request.setStartSeatNumber(1);
        when(venueSectionService.generateSectionSeats(venueId, sectionId, request))
                .thenThrow(new SeatsAlreadyGeneratedException("Seats already exist for this section"));

        ResponseEntity<SectionSeatsResponse> responseEntity = venueSectionController.generateSectionSeats(venueId, sectionId, request);

        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        SectionSeatsResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.FAILURE, response.getResponseStatus());
        assertEquals("Seats already exist for this section", response.getMessage());
    }
}
