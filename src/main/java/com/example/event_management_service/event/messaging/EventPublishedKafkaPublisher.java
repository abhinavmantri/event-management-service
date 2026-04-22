package com.example.event_management_service.event.messaging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class EventPublishedKafkaPublisher {
    private static final String LOG_GROUP = "[EVENT_PUBLISHED_KAFKA_PUBLISHER]";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;
    private final ObjectMapper objectMapper;

    public EventPublishedKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicsProperties kafkaTopicsProperties,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopicsProperties = kafkaTopicsProperties;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EventPublishedDomainEvent domainEvent) {
        String requestId = requestId();
        EventPublishedKafkaMessage message = EventPublishedKafkaMessage.from(domainEvent);
        String topic = kafkaTopicsProperties.getEventPublished();
        String payload = serialize(message);
        log.info("{} request: requestId={}, topic={}, eventId={}", LOG_GROUP, requestId, topic, domainEvent.eventId());
        kafkaTemplate.send(topic, domainEvent.eventId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("{} failure: requestId={}, topic={}, eventId={}", LOG_GROUP, requestId, topic, domainEvent.eventId(), ex);
                        return;
                    }
                    log.info(
                            "{} success: requestId={}, topic={}, eventId={}, partition={}, offset={}",
                            LOG_GROUP,
                            requestId,
                            topic,
                            domainEvent.eventId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }

    private String serialize(EventPublishedKafkaMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize event published message", ex);
        }
    }

    private String requestId() {
        String requestId = MDC.get(REQUEST_ID_MDC_KEY);
        return requestId == null ? "N/A" : requestId;
    }
}
