package com.example.event_management_service;

import com.example.event_management_service.event.messaging.EventPublishedKafkaMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class EventManagementServiceApplicationTests {
	@MockitoBean
	private KafkaTemplate<String, EventPublishedKafkaMessage> kafkaTemplate;

	@Test
	void contextLoads() {
	}

}
