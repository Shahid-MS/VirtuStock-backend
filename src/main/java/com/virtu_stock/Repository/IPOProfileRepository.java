package com.virtu_stock.Repository;

import java.util.List;
import java.util.UUID;

import com.virtu_stock.Projection.LatestGMPProjection;
import com.virtu_stock.Projection.RetailSubscriptionProjection;

public interface IPOProfileRepository {
    List<LatestGMPProjection> findLatestGmp(List<UUID> ipoIds);

    List<RetailSubscriptionProjection> findRetailSubscription(List<UUID> ipoIds);
}
