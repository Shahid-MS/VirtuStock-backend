package com.virtu_stock.DTO.Request;

import java.math.BigDecimal;

import com.virtu_stock.Enum.Verdict;

import com.virtu_stock.Models.IssueSize;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IPOUpdateRequestDTO {

    @NotNull
    private IssueSize issueSize;

    @NotNull
    private Verdict verdict;

    @NotNull
    private BigDecimal rating;

}
