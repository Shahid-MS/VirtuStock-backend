package com.virtu_stock.DTO.Response;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.virtu_stock.Enum.IPOStatus;
import com.virtu_stock.Enum.Verdict;
import com.virtu_stock.Models.GMP;
import com.virtu_stock.Models.IssueSize;

import lombok.Data;

@Data
public class IPOResponseDTO {
    private static final List<String> FIXED_ORDER = List.of("QIB", "Non-Institutional", "Retailer");
    private static final String TOTAL_KEY = "Total";

    private UUID id;
    private String name;
    private String symbol;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate listingDate;
    private Double minPrice;
    private Double listedPrice;
    private Double maxPrice;

    private Integer minQty;

    private String logo;

    private IssueSize issueSize;

    private String about;

    private LocalDate allotmentDate;

    private Verdict verdict;

    private IPOStatus status;

    private List<String> strengths;

    private List<String> risks;

    private LinkedHashMap<String, Double> subscriptions;

    private List<GMP> gmp;
    private Double listingReturn;
    private Double listingReturnPercent;
    private String subscriptionLastUpdated;

    public void normalizeSubscriptionsOrder() {
        if (subscriptions == null || subscriptions.isEmpty())
            return;

        Map<String, Double> current = new LinkedHashMap<>(subscriptions);
        LinkedHashMap<String, Double> ordered = new LinkedHashMap<>();

        for (String key : FIXED_ORDER) {
            ordered.put(key, current.getOrDefault(key, 0.0));
            current.remove(key);
        }

        current.forEach((key, value) -> {
            if (!key.equalsIgnoreCase(TOTAL_KEY)) {
                ordered.put(key, value);
            }
        });
        ordered.put(TOTAL_KEY, current.getOrDefault(TOTAL_KEY, 0.0));
        subscriptions.clear();
        subscriptions.putAll(ordered);
    }
}
