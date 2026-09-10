package com.virtu_stock.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.virtu_stock.Controllers.AppliedIpo;
import com.virtu_stock.Models.AllotedIpo;

public interface AllotedIpoRepository extends JpaRepository<AllotedIpo, UUID> {
    public boolean existsByAppliedIpoId(UUID id);

    public void deleteByAppliedIpo(AppliedIpo appliedIpo);

    @Query("""
                SELECT MONTH(i.startDate) AS month,
                       SUM(
                           CASE
                               WHEN a.sellPrice IS NULL OR a.sellPrice = 0
                                   THEN 0
                               ELSE (a.sellPrice - i.maxPrice - COALESCE(a.taxDeducted, 0))
                                    * a.allotedLot * i.minQty
                           END
                       ) AS totalProfit
                FROM AllotedIpo a
                JOIN a.appliedIpo ap
                JOIN ap.user u
                JOIN ap.ipo i
                WHERE u.id = :userId
                  AND YEAR(i.startDate) = :year
                GROUP BY MONTH(i.startDate)
                ORDER BY month
            """)
    List<Object[]> sumMonthlyProfit(UUID userId, int year);
}
