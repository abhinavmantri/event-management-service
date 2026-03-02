package com.example.event_management_service.venue.controller;

import com.example.event_management_service.shared.model.ResponseStatus;
import com.example.event_management_service.venue.dtos.CreateVenueRequest;
import com.example.event_management_service.venue.dtos.CreateVenueResponse;
import com.example.event_management_service.venue.dtos.VenueListResponse;
import com.example.event_management_service.venue.dtos.VenueResponse;
import com.example.event_management_service.venue.exceptions.VenueExistsException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.model.Venue;
import com.example.event_management_service.venue.service.VenueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueControllerTest {

    @Mock
    private VenueService venueService;

    @InjectMocks
    private VenueController venueController;

    @Test
    void createVenueSuccessReturnsCreatedResponse() throws VenueExistsException {
        CreateVenueRequest request = new CreateVenueRequest();
        request.setName("Grand Hall");
        request.setCity("Mumbai");
        request.setAddress("BKC");

        Venue venue = new Venue();
        when(venueService.createVenue("Grand Hall", "Mumbai", "BKC")).thenReturn(venue);

        ResponseEntity<CreateVenueResponse> responseEntity = venueController.createVenue(request);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        CreateVenueResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.SUCCESS, response.getResponseStatus());
        assertEquals("Venue created successfully", response.getMessage());
        assertSame(venue, response.getVenue());
        verify(venueService).createVenue("Grand Hall", "Mumbai", "BKC");
    }

    @Test
    void createVenueConflictReturnsFailureResponse() throws VenueExistsException {
        CreateVenueRequest request = new CreateVenueRequest();
        request.setName("Grand Hall");
        request.setCity("Mumbai");
        request.setAddress("BKC");

        when(venueService.createVenue("Grand Hall", "Mumbai", "BKC"))
                .thenThrow(new VenueExistsException("Venue already exists for this name, city and address"));

        ResponseEntity<CreateVenueResponse> responseEntity = venueController.createVenue(request);

        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        CreateVenueResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.FAILURE, response.getResponseStatus());
        assertEquals("Venue already exists for this name, city and address", response.getMessage());
    }

    @Test
    void getAllVenuesSuccessReturnsPagedVenueList() {
        Pageable pageable = PageRequest.of(0, 2);
        Venue venue1 = new Venue();
        Venue venue2 = new Venue();
        Page<Venue> venuesPage = new PageImpl<>(List.of(venue1, venue2), pageable, 5);
        when(venueService.searchVenues("hall", pageable)).thenReturn(venuesPage);

        ResponseEntity<VenueListResponse> responseEntity = venueController.getAllVenues("hall", pageable);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        VenueListResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.SUCCESS, response.getResponseStatus());
        assertEquals("Venues fetched successfully", response.getMessage());
        assertEquals(2, response.getVenues().size());
        assertEquals(0, response.getPage());
        assertEquals(2, response.getPageSize());
        assertEquals(5, response.getTotalCount());
        verify(venueService).searchVenues("hall", pageable);
    }

    @Test
    void getVenueByIdSuccessReturnsVenueResponse() throws VenueNotFoundException {
        UUID venueId = UUID.randomUUID();
        Venue venue = new Venue();
        when(venueService.getVenueById(venueId)).thenReturn(venue);

        ResponseEntity<VenueResponse> responseEntity = venueController.getVenueById(venueId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        VenueResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.SUCCESS, response.getResponseStatus());
        assertEquals("Venue fetched successfully", response.getMessage());
        assertSame(venue, response.getVenue());
        verify(venueService).getVenueById(venueId);
    }

    @Test
    void getVenueByIdNotFoundReturnsFailureResponse() throws VenueNotFoundException {
        UUID venueId = UUID.randomUUID();
        when(venueService.getVenueById(venueId)).thenThrow(new VenueNotFoundException("Venue not found"));

        ResponseEntity<VenueResponse> responseEntity = venueController.getVenueById(venueId);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        VenueResponse response = responseEntity.getBody();
        assertEquals(ResponseStatus.FAILURE, response.getResponseStatus());
        assertEquals("Venue not found", response.getMessage());
    }
}
