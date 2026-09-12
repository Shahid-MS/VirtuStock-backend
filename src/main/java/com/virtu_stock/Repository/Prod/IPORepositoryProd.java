package com.virtu_stock.Repository.Prod;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.virtu_stock.Models.IPO;
import com.virtu_stock.Projection.LatestGMPProjection;
import com.virtu_stock.Projection.RetailSubscriptionProjection;
import com.virtu_stock.Repository.IPOProfileRepository;

@Profile("prod")
public interface IPORepositoryProd extends JpaRepository<IPO, UUID>, IPOProfileRepository {
    @Query(value = """
            SELECT
                ipo_id::text AS ipoId,
                gmp,
                gmp_date AS gmpDate,
                last_updated AS lastUpdated
            FROM (
                SELECT
                    ipo_id,
                    gmp,
                    gmp_date,
                    last_updated,
                    ROW_NUMBER() OVER (
                        PARTITION BY ipo_id
                        ORDER BY gmp_date DESC, last_updated DESC
                    ) AS rn
                FROM gmp
                WHERE ipo_id IN (:ipoIds)
            ) x
            WHERE rn = 1
            """, nativeQuery = true)
    List<LatestGMPProjection> findLatestGmp(
            @Param("ipoIds") List<UUID> ipoIds);

    @Query(value = """
            SELECT
                ipo_id::text AS ipoId,
                subscription_value AS subscriptionValue
            FROM subscription
            WHERE name = 'Retail'
              AND ipo_id IN (:ipoIds)
            """, nativeQuery = true)
    List<RetailSubscriptionProjection> findRetailSubscription(
            @Param("ipoIds") List<UUID> ipoIds);
}
