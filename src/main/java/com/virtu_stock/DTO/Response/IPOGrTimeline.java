package com.virtu_stock.DTO.Response;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrTimeline {

    @JsonProperty("open_date")
    private LocalDate openDate;

    @JsonProperty("close_date")
    private LocalDate closeDate;

    @JsonProperty("allotment_date")
    private LocalDate allotmentDate;

    @JsonProperty("refund_date")
    private LocalDate refundDate;

    @JsonProperty("credit_to_demat_date")
    private LocalDate creditToDematDate;

    @JsonProperty("listing_date")
    private LocalDate listingDate;
}