package com.virtu_stock.DTO.Response;

import java.util.UUID;

import lombok.Data;

@Data
public class IPOSearchResponse {
    private UUID id;
    private String name;
    private String symbol;
}
