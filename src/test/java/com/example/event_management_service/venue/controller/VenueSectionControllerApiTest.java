package com.example.event_management_service.venue.controller;

import com.example.event_management_service.shared.service.JWTService;
import com.example.event_management_service.venue.exceptions.InvalidVenueStateException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.exceptions.VenueSectionExistsException;
import com.example.event_management_service.venue.model.VenueSeat;
import com.example.event_management_service.venue.model.VenueSection;
import com.example.event_management_service.venue.service.VenueSectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VenueSectionController.class)
class VenueSectionControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueSectionService venueSectionService;

    @MockitoBean
    private JWTService jwtService;

    @Test
    void createVenueSectionSuccessApiResponse() throws Exception {
        UUID venueId = UUID.randomUUID();
        VenueSection section = new VenueSection();
        section.setId(UUID.randomUUID());
        section.setName("VIP");

        when(venueSectionService.createVenueSection(eq(venueId), eq("VIP"), eq(1))).thenReturn(section);

        mockMvc.perform(post("/venues/{venueId}/sections", venueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"VIP",
                                  "sortOrder":1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Section created successfully"))
                .andExpect(jsonPath("$.venueSection.id").value(section.getId().toString()))
                .andExpect(jsonPath("$.venueSection.name").value("VIP"));
    }

    @Test
    void createVenueSectionConflictApiResponse() throws Exception {
        UUID venueId = UUID.randomUUID();
        when(venueSectionService.createVenueSection(eq(venueId), eq("VIP"), eq(1)))
                .thenThrow(new VenueSectionExistsException("Section already exists for this venue"));

        mockMvc.perform(post("/venues/{venueId}/sections", venueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"VIP",
                                  "sortOrder":1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Section already exists for this venue"));
    }

    @Test
    void generateSectionSeatsSuccessApiResponse() throws Exception {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        VenueSeat seat = new VenueSeat();
        seat.setSeatCode("VIP-R01-S01");
        seat.setRowLabel("R01");
        seat.setSeatNumber(1);

        when(venueSectionService.generateSectionSeats(eq(venueId), eq(sectionId), any()))
                .thenReturn(List.of(seat));

        mockMvc.perform(post("/venues/{venueId}/sections/{sectionId}/seats/generate", venueId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rowCount":2,
                                  "seatsPerRow":2,
                                  "rowLabelType":0,
                                  "startSeatNumber":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Section seats generated successfully"))
                .andExpect(jsonPath("$.seats[0].seatCode").value("VIP-R01-S01"));
    }

    @Test
    void generateSectionSeatsAlphaRowTypeSuccessApiResponse() throws Exception {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        VenueSeat seat = new VenueSeat();
        seat.setSeatCode("VIP-A-S01");
        seat.setRowLabel("A");
        seat.setSeatNumber(1);

        when(venueSectionService.generateSectionSeats(eq(venueId), eq(sectionId), any()))
                .thenReturn(List.of(seat));

        mockMvc.perform(post("/venues/{venueId}/sections/{sectionId}/seats/generate", venueId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rowCount":2,
                                  "seatsPerRow":1,
                                  "rowLabelType":1,
                                  "startSeatNumber":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Section seats generated successfully"))
                .andExpect(jsonPath("$.seats[0].rowLabel").value("A"))
                .andExpect(jsonPath("$.seats[0].seatCode").value("VIP-A-S01"));
    }

    @Test
    void generateSectionSeatsNotFoundApiResponse() throws Exception {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        when(venueSectionService.generateSectionSeats(eq(venueId), eq(sectionId), any()))
                .thenThrow(new VenueNotFoundException("Section not found for venue"));

        mockMvc.perform(post("/venues/{venueId}/sections/{sectionId}/seats/generate", venueId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rowCount":1,
                                  "seatsPerRow":1,
                                  "rowLabelType":0,
                                  "startSeatNumber":1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Section not found for venue"));
    }

    @Test
    void generateSectionSeatsConflictApiResponse() throws Exception {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        when(venueSectionService.generateSectionSeats(eq(venueId), eq(sectionId), any()))
                .thenThrow(new InvalidVenueStateException("Cannot generate seats: venue is used by a published event"));

        mockMvc.perform(post("/venues/{venueId}/sections/{sectionId}/seats/generate", venueId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rowCount":1,
                                  "seatsPerRow":1,
                                  "rowLabelType":0,
                                  "startSeatNumber":1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Cannot generate seats: venue is used by a published event"));
    }

    @Test
    void generateSectionSeatsValidationFailureReturnsBadRequest() throws Exception {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        mockMvc.perform(post("/venues/{venueId}/sections/{sectionId}/seats/generate", venueId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rowCount":0,
                                  "seatsPerRow":1,
                                  "rowLabelType":0,
                                  "startSeatNumber":1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateSectionSeatsValidationFailureWhenRowLabelTypeIsInvalidReturnsBadRequest() throws Exception {
        UUID venueId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();

        mockMvc.perform(post("/venues/{venueId}/sections/{sectionId}/seats/generate", venueId, sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rowCount":1,
                                  "seatsPerRow":1,
                                  "rowLabelType":2,
                                  "startSeatNumber":1
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
