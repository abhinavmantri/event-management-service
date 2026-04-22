package com.example.event_management_service.venue.repository;

import com.example.event_management_service.venue.model.VenueSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VenueSectionRepository extends JpaRepository<VenueSection, UUID> {
    List<VenueSection> findByVenue_Id(UUID venueId);
    boolean existsByVenue_IdAndNameIgnoreCase(UUID venueId, String name);
    boolean existsByIdAndVenue_Id(UUID sectionId, UUID venueId);
    Optional<VenueSection> findByIdAndVenue_Id(UUID sectionId, UUID venueId);
}
