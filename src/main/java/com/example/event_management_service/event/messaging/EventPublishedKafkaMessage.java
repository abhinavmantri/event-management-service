package com.example.event_management_service.event.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventPublishedKafkaMessage(
        String eventType,
        UUID eventId,
        UUID venueId,
        UUID organiserId,
        String organiserEmail,
        String title,
        String category,
        Instant startsAt,
        Instant endsAt,
        Instant publishedAt,
        List<SectionPrice> sectionPrices
) {
    public static final String EVENT_TYPE = "EVENT_PUBLISHED";

    public static EventPublishedKafkaMessage from(EventPublishedDomainEvent domainEvent) {
        return new EventPublishedKafkaMessage(
                EVENT_TYPE,
                domainEvent.eventId(),
                domainEvent.venueId(),
                domainEvent.organiserId(),
                domainEvent.organiserEmail(),
                domainEvent.title(),
                domainEvent.category(),
                domainEvent.startsAt(),
                domainEvent.endsAt(),
                domainEvent.publishedAt(),
                List.copyOf(domainEvent.sectionPrices())
        );
    }

    public record SectionPrice(
            UUID sectionId,
            String sectionName,
            Integer sortOrder,
            Integer priceCents,
            String currency
    ) {
    }
}
