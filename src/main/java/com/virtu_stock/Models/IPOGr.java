package com.virtu_stock.Models;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGr {
    private String slug;
    private String name;

    private String type;
    private String subType;

    @JsonProperty("open_date")
    private LocalDate openDate;

    @JsonProperty("close_date")
    private LocalDate closeDate;

    @JsonProperty("allotment_date")
    private LocalDate allotmentDate;

    @JsonProperty("listing_date")
    private LocalDate listingDate;

    @JsonProperty("price_min")
    private Double priceMin;

    @JsonProperty("price_max")
    private Double priceMax;

    @JsonProperty("lot_size")
    private Integer lotSize;

    private String logo;

    @JsonProperty("issue_price")
    private String issuePrice;

    @JsonProperty("issue_size")
    private String issueSize;

    @JsonProperty("is_listed")
    private Boolean isListed;

    @JsonProperty("listing_price")
    private String listingPrice;

    private IPOGrGMP gmp;
}
