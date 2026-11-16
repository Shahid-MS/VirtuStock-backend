package com.virtu_stock.DTO.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrIssue {

    @JsonProperty("issue_size")
    private String issueSize;

    @JsonProperty("issue_price")
    private String issuePrice;

    @JsonProperty("price_band")
    private String priceBand;

    @JsonProperty("price_min")
    private Double priceMin;

    @JsonProperty("price_max")
    private Double priceMax;

    @JsonProperty("face_value")
    private String faceValue;

    @JsonProperty("lot_size")
    private Integer lotSize;

    @JsonProperty("min_investment")
    private Double minInvestment;

    @JsonProperty("sale_type")
    private String saleType;

    @JsonProperty("listing_on")
    private String listingOn;

    @JsonProperty("bse_scrip_code")
    private String bseScripCode;

    @JsonProperty("nse_symbol")
    private String nseSymbol;
}
