package com.virtu_stock.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GMPInsert(UUID ipoId,
        double gmp,
        LocalDate gmpDate,
        LocalDateTime lastUpdated) {
}
