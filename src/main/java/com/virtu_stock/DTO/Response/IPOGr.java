package com.virtu_stock.DTO.Response;



import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class IPOGr {
    private String slug;
    private String name;
    private String type;
    private IPOGrTimeline timeline;
    private IPOGrIssue issue;
    private IPOGrCompany company;
    private IPOGrContacts contacts;
}
