package com.virtu_stock.DTO.Response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrFinancialRow {

    private String metric;

    private List<String> data;
}
