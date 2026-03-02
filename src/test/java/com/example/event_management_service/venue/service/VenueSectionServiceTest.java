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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueSectionServiceTest {

    @Mock
    private VenueSectionRepository venueSectionRepository;

    @Mock
    private VenueSeatRepository venueSeatRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private VenueSectionService venueSectionService;

    @Test
    void createVenueSectionNormalizesAndPersistsSection() throws VenueNotFoundException, VenueSectionExistsException {
        UUID venueId = UUID.randomUUID();
        Venue venue = new Venue();
        VenueSection savedSection = new VenueSection();
        savedSection.setName("VIP");

        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(venueSectionRepository.existsByVenue_IdAndNameIgnoreCase(venueId, "VIP")).thenReturn(false);
        when(venueSectionRepository.save(any(VenueSection.class))).thenReturn(savedSection);

        VenueSection result = venueSectionService.createVenueSection(venueId, " VIP ", 3);

        ArgumentCaptor<VenueSection> sectionCaptor = ArgumentCaptor.forClass(VenueSection.class);
        verify(venueSectionRepository).save(sectionCaptor.capture());
        VenueSection captured = sectionCaptor.getValue();
        assertEquals("VIP", captured.getName());
        assertEquals(3, captured.getSortOrder());
        assertNotNull(captured.getCreatedAt());
        assertEquals(venue, captured.getVenue());
        assertEquals(savedSection, result);
    }

    @Test
    void generateSectionSeatsThrowsWhenSectionNotFound() {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        GenerateSeatsRequest request = request(2, 2, 0);
        when(venueSectionRepository.findByIdAndVenue_Id(sectionId, venueId)).thenReturn(Optional.empty());

        VenueNotFoundException exception = assertThrows(
                VenueNotFoundException.class,
                () -> venueSectionService.generateSectionSeats(venueId, sectionId, request)
        );

        assertEquals("Section not found for venue", exception.getMessage());
    }

    @Test
    void generateSectionSeatsThrowsWhenVenueHasPublishedEvent() {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(venueId);
        VenueSection section = new VenueSection();
        section.setVenue(venue);
        section.setName("VIP");

        Event publishedEvent = new Event();
        publishedEvent.setStatus(EventStatus.PUBLISHED);
        publishedEvent.setVenue(venue);

        when(venueSectionRepository.findByIdAndVenue_Id(sectionId, venueId)).thenReturn(Optional.of(section));
        when(eventRepository.findAll()).thenReturn(List.of(publishedEvent));

        InvalidVenueStateException exception = assertThrows(
                InvalidVenueStateException.class,
                () -> venueSectionService.generateSectionSeats(venueId, sectionId, request(2, 2, 0))
        );

        assertEquals("Cannot generate seats: venue is used by a published event", exception.getMessage());
    }

    @Test
    void generateSectionSeatsThrowsWhenSeatsAlreadyExist() {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(venueId);
        VenueSection section = new VenueSection();
        section.setVenue(venue);
        section.setName("VIP");

        when(venueSectionRepository.findByIdAndVenue_Id(sectionId, venueId)).thenReturn(Optional.of(section));
        when(eventRepository.findAll()).thenReturn(List.of());
        when(venueSeatRepository.countByVenue_IdAndSection_Id(venueId, sectionId)).thenReturn(2L);

        SeatsAlreadyGeneratedException exception = assertThrows(
                SeatsAlreadyGeneratedException.class,
                () -> venueSectionService.generateSectionSeats(venueId, sectionId, request(2, 2, 0))
        );

        assertEquals("Seats already exist for this section", exception.getMessage());
    }

    @Test
    void generateSectionSeatsCreatesNumericRowsAndSeatCodes() throws VenueNotFoundException, InvalidVenueStateException, SeatsAlreadyGeneratedException {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(venueId);
        VenueSection section = new VenueSection();
        section.setVenue(venue);
        section.setName("VIP");

        when(venueSectionRepository.findByIdAndVenue_Id(sectionId, venueId)).thenReturn(Optional.of(section));
        when(eventRepository.findAll()).thenReturn(List.of());
        when(venueSeatRepository.countByVenue_IdAndSection_Id(venueId, sectionId)).thenReturn(0L);
        when(venueSeatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<VenueSeat> created = venueSectionService.generateSectionSeats(venueId, sectionId, request(2, 3, 0));

        assertEquals(6, created.size());
        assertEquals("R01", created.get(0).getRowLabel());
        assertEquals(1, created.get(0).getSeatNumber());
        assertEquals("VIP-R01-S01", created.get(0).getSeatCode());
        assertEquals("R02", created.get(3).getRowLabel());
        assertEquals(1, created.get(3).getSeatNumber());
        assertEquals("VIP-R02-S01", created.get(3).getSeatCode());
    }

    @Test
    void generateSectionSeatsCreatesAlphaRowsWithAaAbProgression() throws VenueNotFoundException, InvalidVenueStateException, SeatsAlreadyGeneratedException {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(venueId);
        VenueSection section = new VenueSection();
        section.setVenue(venue);
        section.setName("Balcony");

        when(venueSectionRepository.findByIdAndVenue_Id(sectionId, venueId)).thenReturn(Optional.of(section));
        when(eventRepository.findAll()).thenReturn(List.of());
        when(venueSeatRepository.countByVenue_IdAndSection_Id(venueId, sectionId)).thenReturn(0L);
        when(venueSeatRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<VenueSeat> created = venueSectionService.generateSectionSeats(venueId, sectionId, request(28, 1, 1));

        List<String> labels = new ArrayList<>();
        for (VenueSeat seat : created) {
            labels.add(seat.getRowLabel());
        }

        assertEquals(28, created.size());
        assertEquals("A", labels.get(0));
        assertEquals("Z", labels.get(25));
        assertEquals("AA", labels.get(26));
        assertEquals("AB", labels.get(27));
        assertTrue(created.get(26).getSeatCode().startsWith("BALCONY-AA-S"));
    }

    private GenerateSeatsRequest request(int rowCount, int seatsPerRow, int rowLabelType) {
        GenerateSeatsRequest request = new GenerateSeatsRequest();
        request.setRowCount(rowCount);
        request.setSeatsPerRow(seatsPerRow);
        request.setRowLabelType(rowLabelType);
        request.setStartSeatNumber(1);
        return request;
    }
}
