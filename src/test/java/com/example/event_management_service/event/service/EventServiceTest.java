package com.example.event_management_service.event.service;

import com.example.event_management_service.event.exceptions.EventNotFoundException;
import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventStatus;
import com.example.event_management_service.event.repository.EventRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void searchPublicEventsNormalizesInputsAndReturnsPage() {
        Instant startDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2026-12-31T00:00:00Z");
        Pageable pageable = PageRequest.of(1, 10);
        Page<Event> expectedPage = new PageImpl<>(List.of(new Event()), pageable, 1);

        when(eventRepository.searchPublicEvents(
                eq(EventStatus.PUBLISHED),
                any(Instant.class),
                eq(startDate),
                eq(endDate),
                eq("New York"),
                eq("Music"),
                eq("rock"),
                any(Pageable.class)
        )).thenReturn(expectedPage);

        Page<Event> actualPage = eventService.searchPublicEvents(
                " rock ", startDate, endDate, " New York ", " Music ", pageable
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).searchPublicEvents(
                eq(EventStatus.PUBLISHED),
                any(Instant.class),
                eq(startDate),
                eq(endDate),
                eq("New York"),
                eq("Music"),
                eq("rock"),
                pageableCaptor.capture()
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(1, capturedPageable.getPageNumber());
        assertEquals(10, capturedPageable.getPageSize());
        assertSame(expectedPage, actualPage);
    }

    @Test
    void getEventByIdReturnsEventWhenFound() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Event result = eventService.getEventById(eventId);

        assertSame(event, result);
        verify(eventRepository).findById(eventId);
    }

    @Test
    void getEventByIdThrowsWhenEventMissing() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        EventNotFoundException exception = assertThrows(
                EventNotFoundException.class,
                () -> eventService.getEventById(eventId)
        );

        assertEquals("Event not found", exception.getMessage());
        verify(eventRepository).findById(eventId);
    }
}
