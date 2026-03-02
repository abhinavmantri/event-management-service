package com.example.event_management_service.venue.repository;

import com.example.event_management_service.venue.model.VenueSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VenueSeatRepository extends JpaRepository<VenueSeat, UUID> {
    List<VenueSeat> findByVenue_Id(UUID venueId);
    List<VenueSeat> findByVenue_IdAndSection_IdOrderBySeatNumberAsc(UUID venueId, UUID sectionId);
    long countByVenue_IdAndSection_Id(UUID venueId, UUID sectionId);
}
