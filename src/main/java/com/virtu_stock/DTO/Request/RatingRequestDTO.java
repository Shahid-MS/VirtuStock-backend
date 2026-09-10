package com.virtu_stock.DTO.Request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RatingRequestDTO {

    @Min(1)
    @Max(5)
    private Integer rating;

}
