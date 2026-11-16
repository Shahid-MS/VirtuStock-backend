package com.virtu_stock.DTO.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrRegistrar {

    private String name;

    private String slug;

    @JsonProperty("status_page")
    private String statusPage;

}
