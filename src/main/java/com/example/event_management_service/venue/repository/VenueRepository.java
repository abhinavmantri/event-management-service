package com.example.event_management_service.venue.repository;

import com.example.event_management_service.venue.model.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VenueRepository extends JpaRepository<Venue, UUID> {
    boolean existsByNameIgnoreCaseAndCityIgnoreCase(String name, String city);

    @Query("""
            SELECT v FROM Venue v
            WHERE :query IS NULL
               OR LOWER(v.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(v.city) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(v.address) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Venue> searchVenues(@Param("query") String query, Pageable pageable);
}
