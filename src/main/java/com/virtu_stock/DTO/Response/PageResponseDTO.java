package com.virtu_stock.DTO.Response;

import java.util.List;

import lombok.Data;

@Data
public class PageResponseDTO<T> {
    private List<T> content;
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPageElements;
    private Integer totalPages;
    private Long totalElements;
    private boolean lastPage;
}
