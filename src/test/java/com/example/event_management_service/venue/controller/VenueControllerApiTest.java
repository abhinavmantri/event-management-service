package com.example.event_management_service.venue.controller;

import com.example.event_management_service.shared.service.JWTService;
import com.example.event_management_service.venue.exceptions.VenueExistsException;
import com.example.event_management_service.venue.exceptions.VenueNotFoundException;
import com.example.event_management_service.venue.model.Venue;
import com.example.event_management_service.venue.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VenueController.class)
class VenueControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    @MockitoBean
    private JWTService jwtService;

    @Test
    void getAllVenuesSuccessApiResponse() throws Exception {
        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Grand Hall");
        venue.setCity("Mumbai");

        when(venueService.searchVenues(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(venue)));

        mockMvc.perform(get("/venues")
                        .param("query", "hall")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Venues fetched successfully"))
                .andExpect(jsonPath("$.venues[0].name").value("Grand Hall"));
    }

    @Test
    void getVenueByIdSuccessApiResponse() throws Exception {
        UUID venueId = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(venueId);
        venue.setName("City Arena");

        when(venueService.getVenueById(venueId)).thenReturn(venue);

        mockMvc.perform(get("/venues/{venueId}", venueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Venue fetched successfully"))
                .andExpect(jsonPath("$.venue.id").value(venueId.toString()))
                .andExpect(jsonPath("$.venue.name").value("City Arena"));
    }

    @Test
    void getVenueByIdNotFoundApiResponse() throws Exception {
        UUID venueId = UUID.randomUUID();
        when(venueService.getVenueById(venueId)).thenThrow(new VenueNotFoundException("Venue not found"));

        mockMvc.perform(get("/venues/{venueId}", venueId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Venue not found"));
    }

    @Test
    void createVenueUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(post("/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Grand Hall",
                                  "city":"Mumbai",
                                  "address":"BKC"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization token is required"));
    }

    @Test
    void createVenueForbiddenWhenRoleIsNotAllowed() throws Exception {
        when(jwtService.validateAndExtractClaims("token-user")).thenReturn(Map.of("role", "USER"));

        mockMvc.perform(post("/venues")
                        .header("Authorization", "Bearer token-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Grand Hall",
                                  "city":"Mumbai",
                                  "address":"BKC"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Insufficient role for venue endpoint"));
    }

    @Test
    void createVenueSuccessApiResponse() throws Exception {
        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Grand Hall");
        when(jwtService.validateAndExtractClaims("token-admin")).thenReturn(Map.of("role", "ADMIN"));
        when(venueService.createVenue("Grand Hall", "Mumbai", "BKC")).thenReturn(venue);

        mockMvc.perform(post("/venues")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Grand Hall",
                                  "city":"Mumbai",
                                  "address":"BKC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Venue created successfully"))
                .andExpect(jsonPath("$.venue.id").value(venue.getId().toString()))
                .andExpect(jsonPath("$.venue.name").value("Grand Hall"));
    }

    @Test
    void createVenueConflictApiResponse() throws Exception {
        when(jwtService.validateAndExtractClaims("token-organizer")).thenReturn(Map.of("role", "ORGANIZER"));
        when(venueService.createVenue(eq("Grand Hall"), eq("Mumbai"), eq("BKC")))
                .thenThrow(new VenueExistsException("Venue already exists for this name, city and address"));

        mockMvc.perform(post("/venues")
                        .header("Authorization", "Bearer token-organizer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Grand Hall",
                                  "city":"Mumbai",
                                  "address":"BKC"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Venue already exists for this name, city and address"));
    }
}
