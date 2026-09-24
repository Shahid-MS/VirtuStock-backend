package com.virtu_stock.Models;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrGMP {
    private String price;
    private String percentage;
    private String kostak;

    @JsonProperty("subject_to_sauda")
    private String subjectToSauda;

    @JsonProperty("estimated_listing_price")
    private BigDecimal estimatedListingPrice;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("updated_at_label")
    private String updatedAtLabel;
}
