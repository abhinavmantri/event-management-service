package com.example.event_management_service.event.controller;

import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.service.EventService;
import com.example.event_management_service.shared.service.JWTService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private JWTService jwtService;

    @Test
    void browseEventsSuccessApiResponse() throws Exception {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Rock Night");

        when(eventService.searchPublicEvents(anyString(), any(), any(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        mockMvc.perform(get("/events")
                        .param("query", "rock")
                        .param("city", "Delhi")
                        .param("category", "Music")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Events fetched successfully"))
                .andExpect(jsonPath("$.events[0].title").value("Rock Night"));
    }

    @Test
    void browseEventsFailureApiResponse() throws Exception {
        when(eventService.searchPublicEvents(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Search failed"));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Search failed"));
    }

    @Test
    void getEventByIdSuccessApiResponse() throws Exception {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        event.setTitle("Tech Summit");

        when(eventService.getEventById(eventId)).thenReturn(event);

        mockMvc.perform(get("/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Event fetched successfully"))
                .andExpect(jsonPath("$.event.id").value(eventId.toString()))
                .andExpect(jsonPath("$.event.title").value("Tech Summit"));
    }

    @Test
    void getEventByIdNotFoundApiResponse() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(eventService.getEventById(eventId)).thenThrow(new EventNotFoundException("Event not found"));

        mockMvc.perform(get("/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Event not found"));
    }
}
