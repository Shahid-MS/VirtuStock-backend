package com.virtu_stock.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.virtu_stock.Projection.IPORegistrarProjection;
import com.virtu_stock.Projection.LatestGMPProjection;
import com.virtu_stock.Projection.RetailSubscriptionProjection;
import com.virtu_stock.Projection.SubscriptionProjection;

public interface IPOProfileRepository {
    List<LatestGMPProjection> findLatestGmp(List<UUID> ipoIds);

    List<RetailSubscriptionProjection> findRetailSubscription(List<UUID> ipoIds);

    List<SubscriptionProjection> findSubscription(List<UUID> ipoIds);

    @Query("""
                SELECT i.id AS ipoId, i.registrar AS registrar
                FROM IPO i
                WHERE i.id IN :ipoIds
                AND i.registrar IS NOT NULL
            """)
    List<IPORegistrarProjection> findRegistrars(
            @Param("ipoIds") List<UUID> ipoIds);
}
