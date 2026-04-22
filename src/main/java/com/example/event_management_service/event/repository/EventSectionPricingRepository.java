package com.example.event_management_service.event.repository;

import com.example.event_management_service.event.model.EventSectionPricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface EventSectionPricingRepository extends JpaRepository<EventSectionPricing, UUID> {
    List<EventSectionPricing> findByEvent_Id(UUID eventId);
}
