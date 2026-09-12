package com.virtu_stock.Models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.virtu_stock.Enum.IPOStatus;
import com.virtu_stock.Enum.Verdict;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ipo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IPO {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ipo_alert_id")
    private String ipoAlertId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotBlank(message = "Type is required")
    private String type;

    @Column(name = "start_date")
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date")
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Allotment date is required")
    @Column(name = "allotment_date")
    private LocalDate allotmentDate;

    @Column(name = "listing_date")
    @NotNull(message = "Listing date is required")
    private LocalDate listingDate;

    @NotNull(message = "Minimum Price is required")
    @Column(name = "min_price")
    private Double minPrice;

    @Column(name = "max_price")
    @NotNull(message = "Maximum Price is required")
    private Double maxPrice;

    @Column(name = "listed_price")
    private Double listedPrice;

    @Column(name = "minimum_quantity")
    @NotNull(message = "Minimum Quantity Price is required")
    private Integer minQty;

    @NotBlank(message = "logo url is required")
    private String logo;

    @Column(name = "issue_size")
    private IssueSize issueSize;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(name = "subscription-lastUpdated")
    private LocalDateTime subscriptionLastUpdated;

    @Enumerated(EnumType.STRING)
    private Verdict verdict;

    @ElementCollection
    @CollectionTable(name = "ipo_strengths", joinColumns = @JoinColumn(name = "ipo_id"))
    @Column(name = "strength")
    private List<String> strengths;

    @ElementCollection
    @CollectionTable(name = "ipo_risks", joinColumns = @JoinColumn(name = "ipo_id"))
    @Column(name = "risk")
    private List<String> risks;

    @ElementCollection
    @CollectionTable(name = "subscription", joinColumns = @JoinColumn(name = "ipo_id"))
    @MapKeyColumn(name = "name")
    @Column(name = "subscription_value")
    private Map<String, Double> subscriptions;

    @ElementCollection
    @CollectionTable(name = "gmp", joinColumns = @JoinColumn(name = "ipo_id"))
    @OrderBy("gmpDate Desc")
    private List<GMP> gmp;

    @PrePersist
    public void initializeDefaults() {
        // Subscriptions (Map)
        if (subscriptions == null || subscriptions.isEmpty()) {
            subscriptions = new HashMap<>();
            subscriptions.put("QIB", 0.0);
            subscriptions.put("Non-Institutional", 0.0);
            subscriptions.put("Retailer", 0.0);
            subscriptions.put("Total", 0.0);
        }
        // GMP
        if (gmp == null || gmp.isEmpty()) {
            gmp = new ArrayList<>();
            gmp.add(new GMP(0, LocalDate.now(), LocalDateTime.now()));
        }

        // Verdict
        if (verdict == null) {
            verdict = Verdict.NOT_REVIEWED;
        }
        subscriptionLastUpdated = LocalDateTime.now();
    }

    @Transient
    @JsonProperty("status")
    public IPOStatus getStatus() {
        LocalDate today = LocalDate.now();
        if (startDate == null || endDate == null) {
            return null;
        }

        if (today.isAfter(listingDate)) {
            return IPOStatus.CLOSED;
        }

        if (today.isEqual(listingDate)) {
            LocalTime nowTime = LocalTime.now();
            if (nowTime.isAfter(LocalTime.of(10, 0))) {
                return IPOStatus.LISTED;
            } else {
                return IPOStatus.LISTING_PENDING;
            }
        }

        if (today.isAfter(allotmentDate)) {
            return IPOStatus.LISTING_PENDING;
        }

        if (today.isEqual(allotmentDate)) {
            LocalTime nowTime = LocalTime.now();
            if (nowTime.isAfter(LocalTime.of(17, 0))) {
                return IPOStatus.LISTING_PENDING;
            } else {
                return IPOStatus.ALLOTMENT;
            }
        }

        if (today.isAfter(endDate)) {
            return IPOStatus.ALLOTMENT_PENDING;
        }

        if (today.isEqual(endDate)) {
            LocalTime nowTime = LocalTime.now();
            if (nowTime.isAfter(LocalTime.of(17, 0))) {
                return IPOStatus.LISTING_PENDING;
            } else {
                return IPOStatus.OPEN;
            }
        }

        if (today.isAfter(startDate)) {
            return IPOStatus.OPEN;
        }

        if (today.isEqual(startDate)) {
            LocalTime nowTime = LocalTime.now();
            if (nowTime.isAfter(LocalTime.of(9, 0))) {
                return IPOStatus.OPEN;
            }
        }
        return IPOStatus.UPCOMING;
    }

    @Transient
    @JsonProperty("listingReturn")
    public Double getListingReturn() {
        if (listedPrice == null) {
            return 0.0;
        }
        return listedPrice - maxPrice;
    }

    @Transient
    @JsonProperty("listingReturnPercent")
    public Double getListingReturnPercent() {
        Double listingReturn = getListingReturn();
        if (listingReturn == 0.0) {
            return 0.0;
        }
        Double listingReturnPercent = (listingReturn / maxPrice) * 100;
        return Math.round(listingReturnPercent * 100.0) / 100.0;
    }

}
