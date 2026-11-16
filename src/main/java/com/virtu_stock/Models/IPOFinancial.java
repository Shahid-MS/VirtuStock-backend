package com.virtu_stock.Models;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ipo_financial", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ipo_financial", columnNames = "ipo_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IPOFinancial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ipo_id", nullable = false, unique = true)
    @JsonIgnore
    private IPO ipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<LocalDate, Map<String, BigDecimal>> financials;

    @Column(length = 50)
    private String unit;
}