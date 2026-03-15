package com.example.event_management_service.event.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventPublishedDomainEvent(
        UUID eventId,
        UUID venueId,
        UUID organiserId,
        String organiserEmail,
        String title,
        String category,
        Instant startsAt,
        Instant endsAt,
        Instant publishedAt,
        List<EventPublishedKafkaMessage.SectionPrice> sectionPrices
) {
}
