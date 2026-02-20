package com.example.event_management_service.event.model;

import com.example.event_management_service.shared.model.BaseEntity;
import com.example.event_management_service.venue.model.VenueSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_section_pricing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSectionPricing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private VenueSection section;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}
