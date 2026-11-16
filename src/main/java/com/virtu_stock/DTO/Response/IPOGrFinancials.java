package com.virtu_stock.DTO.Response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class IPOGrFinancials {

    private List<String> headers;

    private List<IPOGrFinancialRow> rows;

    private String unit;
}