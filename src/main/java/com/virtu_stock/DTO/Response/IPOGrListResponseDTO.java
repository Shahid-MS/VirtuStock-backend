package com.virtu_stock.DTO.Response;

import java.util.List;



import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrListResponseDTO {
    private boolean success;
    private String plan;
    private int count;
    private List<IPOGrList> data;
}
