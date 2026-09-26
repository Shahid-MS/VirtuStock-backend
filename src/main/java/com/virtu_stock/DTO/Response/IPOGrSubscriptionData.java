package com.virtu_stock.DTO.Response;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class IPOGrSubscriptionData {
    private String slug;
    private String name;
    private String type;
    private IPOGrSubscriptionSummary summary;
}
