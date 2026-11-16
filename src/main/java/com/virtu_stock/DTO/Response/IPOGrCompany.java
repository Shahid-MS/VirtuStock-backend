package com.virtu_stock.DTO.Response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IPOGrCompany {

    private String about;
    private List<String> strengths;
    private List<String> risks;
    private IPOGrFinancials financials;
}