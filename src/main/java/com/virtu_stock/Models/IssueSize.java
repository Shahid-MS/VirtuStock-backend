package com.virtu_stock.Models;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueSize {
    private String fresh;
    private String offerForSale;
    private String totalIssueSize;
}
