package com.virtu_stock.DTO.Response;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class IPOGrFetchResult {
    private final List<IPOGrDTO.Saved> saved = new ArrayList<>();
    private final List<IPOGrDTO.Skipped> skipped = new ArrayList<>();
    private final List<IPOGrDTO.Exists> exists = new ArrayList<>();
    private final List<IPOGrDTO.Error> errors = new ArrayList<>();
}
