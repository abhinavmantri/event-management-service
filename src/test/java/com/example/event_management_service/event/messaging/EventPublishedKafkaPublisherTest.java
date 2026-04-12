package com.example.event_management_service.event.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPublishedKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, EventPublishedKafkaMessage> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties kafkaTopicsProperties;

    @InjectMocks
    private EventPublishedKafkaPublisher eventPublishedKafkaPublisher;

    @Test
    void publishSendsKafkaMessageToConfiguredTopic() {
        UUID eventId = UUID.randomUUID();
        EventPublishedDomainEvent domainEvent = new EventPublishedDomainEvent(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "org@example.com",
                "Rock Night",
                "Music",
                Instant.parse("2026-04-01T18:00:00Z"),
                Instant.parse("2026-04-01T21:00:00Z"),
                Instant.parse("2026-03-15T10:00:00Z"),
                List.of(new EventPublishedKafkaMessage.SectionPrice(UUID.randomUUID(), "VIP", 1, 5000, "INR")),
                List.of(new EventPublishedKafkaMessage.SeatInventory(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "VIP-R01-S01",
                        "R01",
                        1,
                        5000,
                        "INR"
                ))
        );
        EventPublishedKafkaMessage expectedMessage = EventPublishedKafkaMessage.from(domainEvent);

        CompletableFuture<SendResult<String, EventPublishedKafkaMessage>> future = new CompletableFuture<>();
        SendResult<String, EventPublishedKafkaMessage> sendResult = mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata recordMetadata =
                mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(12L);
        future.complete(sendResult);

        when(kafkaTopicsProperties.getEventPublished()).thenReturn("event.published.v1");
        when(kafkaTemplate.send("event.published.v1", eventId.toString(), expectedMessage))
                .thenReturn(future);

        eventPublishedKafkaPublisher.publish(domainEvent);

        ArgumentCaptor<EventPublishedKafkaMessage> messageCaptor = ArgumentCaptor.forClass(EventPublishedKafkaMessage.class);
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("event.published.v1"),
                org.mockito.ArgumentMatchers.eq(eventId.toString()),
                messageCaptor.capture()
        );

        EventPublishedKafkaMessage message = messageCaptor.getValue();
        assertEquals(EventPublishedKafkaMessage.EVENT_TYPE, message.eventType());
        assertEquals(eventId, message.eventId());
        assertEquals(domainEvent.sectionPrices(), message.sectionPrices());
        assertEquals(domainEvent.seats(), message.seats());
    }
}
