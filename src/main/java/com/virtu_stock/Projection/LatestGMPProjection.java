package com.virtu_stock.Projection;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface LatestGMPProjection {
    String getIpoId();

    Double getGmp();

    LocalDate getGmpDate();

    LocalDateTime getLastUpdated();
}
