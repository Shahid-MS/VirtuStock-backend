package com.virtu_stock.DTO.Response;

import java.util.List;

import com.virtu_stock.Models.IPOGr;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrResponseDTO {
    private boolean success;
    private String plan;
    private int count;
    private List<IPOGr> data;
}
