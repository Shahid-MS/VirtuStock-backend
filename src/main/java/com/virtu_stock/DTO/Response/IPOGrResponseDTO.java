package com.virtu_stock.DTO.Response;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrResponseDTO {
    private boolean success;
    private String plan;
    private IPOGr data;
}
