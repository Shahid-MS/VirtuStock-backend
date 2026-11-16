package com.virtu_stock.Projection;

import java.util.UUID;

public interface IPOUpdateProjection {
    UUID getId();

    String getName();

    String getIpoGrSlug();
}
