package com.virtu_stock.DTO.Response;

import lombok.Getter;

import lombok.Setter;

@Getter
@Setter
public class RegistrarResponseDTO {

    private Long id;

    private String name;

    private String slug;

    private String url;

    private String logo;
}
