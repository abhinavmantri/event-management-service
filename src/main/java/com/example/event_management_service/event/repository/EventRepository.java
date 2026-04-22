package com.example.event_management_service.event.repository;

import com.example.event_management_service.event.model.Event;
import com.example.event_management_service.event.model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    java.util.Optional<Event> findByIdAndOrganiserId(UUID id, UUID organiserId);

    java.util.Optional<Event> findByIdAndStatus(UUID id, EventStatus status);

    boolean existsByOrganiserIdAndVenue_IdAndTitleIgnoreCaseAndStartsAt(
            UUID organiserId,
            UUID venueId,
            String title,
            Instant startsAt
    );

    @Query("""
            SELECT e
            FROM Event e
            JOIN e.venue v
            WHERE e.status = :status
              AND e.startsAt >= :now
              AND (:startDate IS NULL OR e.startsAt >= :startDate)
              AND (:endDate IS NULL OR e.startsAt <= :endDate)
              AND (:city IS NULL OR lower(v.city) = lower(:city))
              AND (:category IS NULL OR lower(e.category) = lower(:category))
              AND (
                   :query IS NULL
                OR lower(e.title) LIKE lower(concat('%', :query, '%'))
                OR lower(e.description) LIKE lower(concat('%', :query, '%'))
                OR lower(e.category) LIKE lower(concat('%', :query, '%'))
              )
            ORDER BY e.startsAt ASC
            """)
    Page<Event> searchPublicEvents(
            @Param("status") EventStatus status,
            @Param("now") Instant now,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("city") String city,
            @Param("category") String category,
            @Param("query") String query,
            Pageable pageable
    );
}
