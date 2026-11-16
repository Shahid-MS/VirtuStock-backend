package com.virtu_stock.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GMPUpdate(
        UUID ipoId,
        LocalDate gmpDate,
        double gmp,
        LocalDateTime lastUpdated) {
}