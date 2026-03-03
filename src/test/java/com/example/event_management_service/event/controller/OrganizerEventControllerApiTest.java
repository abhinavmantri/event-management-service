package com.example.event_management_service.event.controller;

import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.exceptions.InvalidEventStateException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventSectionPricing;
import com.example.event_management_service.event.service.OrganizerEventService;
import com.example.event_management_service.shared.service.JWTService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizerEventController.class)
class OrganizerEventControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizerEventService organizerEventService;

    @MockitoBean
    private JWTService jwtService;

    @Test
    void createEventUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(post("/organizer/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "venueId": "%s",
                                  "title": "Rock Night",
                                  "startsAt": "%s"
                                }
                                """.formatted(UUID.randomUUID(), Instant.now().plusSeconds(7200))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization token is required"));
    }

    @Test
    void createEventForbiddenWhenRoleIsNotOrganizer() throws Exception {
        when(jwtService.validateAndExtractClaims("token-user")).thenReturn(Map.of("role", "USER"));

        mockMvc.perform(post("/organizer/events")
                        .header("Authorization", "Bearer token-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "venueId": "%s",
                                  "title": "Rock Night",
                                  "startsAt": "%s"
                                }
                                """.formatted(UUID.randomUUID(), Instant.now().plusSeconds(7200))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Insufficient role for organizer endpoint"));
    }

    @Test
    void createEventSuccessApiResponse() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        when(jwtService.validateAndExtractClaims("token-org")).thenReturn(Map.of(
                "role", "ORGANIZER",
                "id", UUID.randomUUID().toString(),
                "email", "org@example.com"
        ));
        Event savedEvent = new Event();
        savedEvent.setId(eventId);
        savedEvent.setTitle("Rock Night");
        when(organizerEventService.createEvent(any(), any())).thenReturn(savedEvent);

        mockMvc.perform(post("/organizer/events")
                        .header("Authorization", "Bearer token-org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "venueId": "%s",
                                  "title": "Rock Night",
                                  "startsAt": "%s"
                                }
                                """.formatted(venueId, Instant.now().plusSeconds(7200))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Event created successfully"))
                .andExpect(jsonPath("$.event.id").value(eventId.toString()));
    }

    @Test
    void updateEventNotFoundApiResponse() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(jwtService.validateAndExtractClaims("token-org")).thenReturn(Map.of(
                "role", "ORGANIZER",
                "id", UUID.randomUUID().toString(),
                "email", "org@example.com"
        ));
        when(organizerEventService.updateEvent(eq(eventId), any()))
                .thenThrow(new EventNotFoundException("Event not found"));

        mockMvc.perform(patch("/organizer/events/{eventId}", eventId)
                        .header("Authorization", "Bearer token-org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Event not found"));
    }

    @Test
    void configurePricingSuccessApiResponse() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        when(jwtService.validateAndExtractClaims("token-org")).thenReturn(Map.of(
                "role", "ORGANIZER",
                "id", UUID.randomUUID().toString(),
                "email", "org@example.com"
        ));
        EventSectionPricing pricing = new EventSectionPricing();
        pricing.setId(UUID.randomUUID());
        pricing.setCurrency("INR");
        pricing.setPriceCents(1500);
        when(organizerEventService.configureEventPricing(eq(eventId), any())).thenReturn(List.of(pricing));

        mockMvc.perform(post("/organizer/events/{eventId}/pricing", eventId)
                        .header("Authorization", "Bearer token-org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currency": "INR",
                                  "prices": [
                                    {
                                      "sectionId": "%s",
                                      "priceCents": 1500
                                    }
                                  ]
                                }
                                """.formatted(sectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Event pricing configured successfully"))
                .andExpect(jsonPath("$.pricings[0].currency").value("INR"))
                .andExpect(jsonPath("$.pricings[0].priceCents").value(1500));
    }

    @Test
    void initializeInventoryBadRequestApiResponse() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(jwtService.validateAndExtractClaims("token-org")).thenReturn(Map.of(
                "role", "ORGANIZER",
                "id", UUID.randomUUID().toString(),
                "email", "org@example.com"
        ));
        when(organizerEventService.initializeEventInventory(eventId))
                .thenThrow(new InvalidEventStateException("Configure event pricing before initializing inventory"));

        mockMvc.perform(post("/organizer/events/{eventId}/inventory/init", eventId)
                        .header("Authorization", "Bearer token-org"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Configure event pricing before initializing inventory"));
    }
}
