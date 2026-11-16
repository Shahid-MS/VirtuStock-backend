package com.virtu_stock.DTO.Response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrSubscriptionSummary {
    private String total;
    private String qib;
    private String nii;
    private String retail;
    private String employee;
    private String shareholder;
    private String anchor;
    private String applications;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("updated_at_label")
    private String updatedAtLabel;
}
