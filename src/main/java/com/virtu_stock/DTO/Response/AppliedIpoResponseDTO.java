package com.virtu_stock.DTO.Response;

import java.time.LocalDate;
import java.util.UUID;

import com.virtu_stock.Enum.AllotmentStatus;

import lombok.Data;

@Data
public class AppliedIpoResponseDTO {
    private UUID id;
    private IPOResponseDTO ipo;
    private Integer appliedLot;
    private AllotmentStatus allotment;
    private LocalDate appliedDate;
    private AllotedIPOResponseDTO allotedIpo;
}
