package com.virtu_stock.DTO.Response;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;

@Data
public class AllotedIPOResponseDTO {
    private UUID id;
    private Integer allotedLot;
    private Double sellPrice;
    private Double taxDeducted;
    private LocalDate allotedDate;
    private Double netReturn;
    private Double netReturnPercent;
}
