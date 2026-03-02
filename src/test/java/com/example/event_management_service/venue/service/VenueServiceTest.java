package com.example.event_management_service.venue.service;

import com.example.event_management_service.venue.exceptions.VenueExistsException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.model.Venue;
import com.example.event_management_service.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private VenueService venueService;

    @Test
    void createVenueNormalizesInputAndPersistsVenue() {
        Venue savedVenue = Venue.builder().name("Grand Hall").city("Mumbai").address("BKC").build();
        when(venueRepository.existsByNameIgnoreCaseAndCityIgnoreCase("Grand Hall", "Mumbai")).thenReturn(false);
        when(venueRepository.save(any(Venue.class))).thenReturn(savedVenue);

        Venue result = venueService.createVenue(" Grand Hall ", " Mumbai ", " BKC ");

        ArgumentCaptor<Venue> venueCaptor = ArgumentCaptor.forClass(Venue.class);
        verify(venueRepository).save(venueCaptor.capture());
        Venue capturedVenue = venueCaptor.getValue();

        assertEquals("Grand Hall", capturedVenue.getName());
        assertEquals("Mumbai", capturedVenue.getCity());
        assertEquals("BKC", capturedVenue.getAddress());
        assertNotNull(capturedVenue.getCreatedAt());
        assertNotNull(capturedVenue.getUpdatedAt());
        assertTrue(!capturedVenue.getUpdatedAt().isBefore(capturedVenue.getCreatedAt()));
        assertSame(savedVenue, result);
    }

    @Test
    void createVenueThrowsWhenVenueAlreadyExists() {
        when(venueRepository.existsByNameIgnoreCaseAndCityIgnoreCase("Grand Hall", "Mumbai")).thenReturn(true);

        VenueExistsException exception = assertThrows(
                VenueExistsException.class,
                () -> venueService.createVenue(" Grand Hall ", " Mumbai ", " BKC ")
        );

        assertEquals("Venue already exists for this name, city and address", exception.getMessage());
    }

    @Test
    void listVenuesReturnsAllVenues() {
        List<Venue> venues = List.of(new Venue(), new Venue());
        when(venueRepository.findAll()).thenReturn(venues);

        List<Venue> result = venueService.listVenues();

        assertSame(venues, result);
        verify(venueRepository).findAll();
    }

    @Test
    void searchVenuesNormalizesInputsAndReturnsPage() {
        Pageable pageable = PageRequest.of(1, 5);
        Page<Venue> expectedPage = new PageImpl<>(List.of(new Venue()), pageable, 1);
        when(venueRepository.searchVenues(eq("arena"), any(Pageable.class))).thenReturn(expectedPage);

        Page<Venue> actualPage = venueService.searchVenues(" arena ", pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(venueRepository).searchVenues(eq("arena"), pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
        assertSame(expectedPage, actualPage);
    }

    @Test
    void searchVenuesTreatsBlankQueryAsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Venue> expectedPage = new PageImpl<>(List.of(), pageable, 0);
        when(venueRepository.searchVenues(any(), any(Pageable.class))).thenReturn(expectedPage);

        venueService.searchVenues("   ", pageable);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(venueRepository).searchVenues(queryCaptor.capture(), any(Pageable.class));
        assertNull(queryCaptor.getValue());
    }

    @Test
    void getVenueByIdReturnsVenueWhenFound() {
        UUID venueId = UUID.randomUUID();
        Venue venue = new Venue();
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));

        Venue result = venueService.getVenueById(venueId);

        assertSame(venue, result);
        verify(venueRepository).findById(venueId);
    }

    @Test
    void getVenueByIdThrowsWhenVenueMissing() {
        UUID venueId = UUID.randomUUID();
        when(venueRepository.findById(venueId)).thenReturn(Optional.empty());

        VenueNotFoundException exception = assertThrows(
                VenueNotFoundException.class,
                () -> venueService.getVenueById(venueId)
        );

        assertEquals("Venue not found", exception.getMessage());
        verify(venueRepository).findById(venueId);
    }
}
