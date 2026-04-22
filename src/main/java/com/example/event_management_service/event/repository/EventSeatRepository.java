package com.example.event_management_service.event.repository;

import com.example.event_management_service.event.model.EventSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {
    long countByEvent_Id(UUID eventId);
    java.util.List<EventSeat> findByEvent_Id(UUID eventId);
}
