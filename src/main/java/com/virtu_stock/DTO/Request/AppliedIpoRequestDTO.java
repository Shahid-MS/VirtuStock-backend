package com.virtu_stock.DTO.Request;

import java.util.UUID;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppliedIpoRequestDTO {

    @NotNull(message = "IPO ID is required")
    private UUID ipoId;
    @NotNull(message = "Applied lot is required")
    @Min(value = 1, message = "Applied lot must be at least 1")
    private Integer appliedLot;
}