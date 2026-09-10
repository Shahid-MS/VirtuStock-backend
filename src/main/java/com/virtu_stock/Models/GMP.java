package com.virtu_stock.Models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
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
public class GMP {

    private double gmp;

    @Column(name = "gmp_date")
    private LocalDate gmpDate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

}
