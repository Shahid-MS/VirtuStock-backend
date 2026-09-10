package com.virtu_stock.DTO.Response;

import java.util.Map;

import lombok.Data;

@Data
public class RatingResponseDTO {
    private Double averageRating;
    private Long totalUsers;
    private Map<Integer, Long> distribution;
}
