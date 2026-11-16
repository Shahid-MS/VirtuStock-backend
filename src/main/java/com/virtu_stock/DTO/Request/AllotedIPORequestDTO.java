package com.virtu_stock.DTO.Request;

import com.virtu_stock.Models.AppliedIpo;

import lombok.Data;

@Data
public class AllotedIPORequestDTO {
    private AppliedIpo appliedIpo;
    private Integer allotedLot;
    private Double sellPrice;
    private Double taxDeducted;
}
