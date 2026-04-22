package com.example.event_management_service.event.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

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
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties kafkaTopicsProperties;

    private EventPublishedKafkaPublisher eventPublishedKafkaPublisher;

    @Test
    void publishSendsKafkaMessageToConfiguredTopic() {
        eventPublishedKafkaPublisher = new EventPublishedKafkaPublisher(
                kafkaTemplate,
                kafkaTopicsProperties,
                new ObjectMapper()
        );
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

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        SendResult<String, String> sendResult = mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata recordMetadata =
                mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(12L);
        future.complete(sendResult);

        when(kafkaTopicsProperties.getEventPublished()).thenReturn("event.published.v1");
        when(kafkaTemplate.send(
                org.mockito.ArgumentMatchers.eq("event.published.v1"),
                org.mockito.ArgumentMatchers.eq(eventId.toString()),
                org.mockito.ArgumentMatchers.anyString()
        ))
                .thenReturn(future);

        eventPublishedKafkaPublisher.publish(domainEvent);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                org.mockito.ArgumentMatchers.eq("event.published.v1"),
                org.mockito.ArgumentMatchers.eq(eventId.toString()),
                messageCaptor.capture()
        );

        String payload = messageCaptor.getValue();
        assertEquals(true, payload.contains("\"eventType\":\"" + EventPublishedKafkaMessage.EVENT_TYPE + "\""));
        assertEquals(true, payload.contains("\"eventId\":\"" + eventId + "\""));
        assertEquals(true, payload.contains("\"seatCode\":\"VIP-R01-S01\""));
    }
}
