package com.virtu_stock.DTO.Response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckAppliedIpoResponseDTO {
    private boolean applied;
    private UUID appliedIpoId;
}
