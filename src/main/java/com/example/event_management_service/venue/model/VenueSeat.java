package com.example.event_management_service.venue.model;

import com.example.event_management_service.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "venue_seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private VenueSection section;

    @Column(name = "seat_code", nullable = false, length = 40)
    private String seatCode;

    @Column(name = "row_label", length = 20)
    private String rowLabel;

    @Column(name = "seat_number")
    private Integer seatNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
