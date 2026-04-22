package com.example.event_management_service.event.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class EventPricingRequest {
    @NotBlank(message = "currency is required")
    private String currency;

    @NotEmpty(message = "prices must not be empty")
    @Valid
    private List<PriceItem> prices;

    @Data
    public static class PriceItem {
        @NotNull(message = "sectionId is required")
        private UUID sectionId;

        @NotNull(message = "priceCents is required")
        @Min(value = 1, message = "priceCents must be greater than 0")
        private Integer priceCents;
    }
}
