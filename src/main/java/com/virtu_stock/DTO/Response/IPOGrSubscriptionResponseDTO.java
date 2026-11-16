package com.virtu_stock.DTO.Response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrSubscriptionResponseDTO {
    private boolean success;
    private String plan;
    private IPOGrSubscriptionData data;
}
