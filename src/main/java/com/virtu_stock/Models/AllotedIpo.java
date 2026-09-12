package com.virtu_stock.Models;

import java.beans.Transient;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.virtu_stock.Controllers.AppliedIpo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alloted_ipo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AllotedIpo {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_ipo_id", nullable = false, unique = true)
    private AppliedIpo appliedIpo;

    @Column(nullable = false)
    private Integer allotedLot;

    @Column(name = "sell_price")
    private Double sellPrice;

    @Column(name = "tax_deducted")
    private Double taxDeducted;

    @Column(name = "alloted_date")
    private LocalDate allotedDate;

    @Transient
    @JsonProperty("netReturn")
    public Double getNetReturn() {
        if (sellPrice == null || appliedIpo == null || appliedIpo.getIpo() == null) {
            return 0.0;
        }
        Double maxPrice = appliedIpo.getIpo().getMaxPrice();
        return (sellPrice - maxPrice) - (taxDeducted != null ? taxDeducted : 0.0);
    }

    @Transient
    @JsonProperty("netReturnPercent")
    public Double getNetReturnPercent() {
        Double netReturn = getNetReturn();
        if (netReturn == 0.0) {
            return 0.0;
        }
        Double totalInvested = appliedIpo.getIpo().getMaxPrice();
        Double netReturnPercent = (netReturn / totalInvested) * 100;
        return Math.round(netReturnPercent * 100.0) / 100.0;
    }

    @PrePersist
    public void initialze() {
        this.allotedDate = LocalDate.now();
    }
}
