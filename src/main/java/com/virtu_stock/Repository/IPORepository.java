package com.virtu_stock.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.virtu_stock.Models.IPO;
import com.virtu_stock.Projection.IPOUpdateProjection;

public interface IPORepository extends JpaRepository<IPO, UUID>, JpaSpecificationExecutor<IPO> {

    public List<IPO> findAllByOrderByEndDateDesc();

    public List<IPO> findByListingDateLessThanEqual(LocalDate date);

    long countByStartDateBetween(LocalDate start, LocalDate end);

    List<IPO> findByIpoGrSlugIn(Set<String> ipoGrSlugs);

    List<IPO> findByIpoGrSlugIsNull();

    List<IPO> findByIpoAlertIdIn(List<String> ipoAlertIds);

    List<IPO> findByIpoAlertIdIsNull();

    public List<IPO> findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCaseOrderByName(String name, String symbol);

    @Query("""
                SELECT MONTH(i.startDate), COUNT(i)
                FROM IPO i
                WHERE YEAR(i.startDate) = :year
                GROUP BY MONTH(i.startDate)
            """)
    List<Object[]> countIpoByMonthAndYear(@Param("year") int year);

    @Query("""
                SELECT i FROM IPO i
                WHERE i.subscriptionLastUpdated >= :fromDate
            """)
    List<IPO> findIposUpdatedSince(@Param("fromDate") LocalDateTime fromDate);

    @Query("""
                SELECT i
                FROM IPO i
                WHERE i.ipoGrSlug IS NOT NULL
                  AND i.startDate IS NOT NULL
                  AND i.endDate IS NOT NULL
                  AND i.startDate <= :today
                  AND i.endDate >= :today
            """)
    List<IPO> findOpenIposForSubscription(LocalDate today);

    @Query("""
            SELECT i.id AS id,
                   i.name AS name,
                   i.ipoGrSlug AS ipoGrSlug
            FROM IPO i
            WHERE i.ipoGrSlug IS NOT NULL
              AND i.startDate IS NOT NULL
              AND i.endDate IS NOT NULL
              AND CURRENT_DATE <= i.listingDate
            ORDER BY i.startDate DESC
            """)
    List<IPOUpdateProjection> findUpcomingToListingIPOs();

    @Query("""
            SELECT i
            FROM IPO i
            WHERE i.id IN :ids
            """)
    List<IPO> findAllForBulkUpdate(@Param("ids") List<UUID> ids);

    @Query("""
            SELECT DISTINCT i
            FROM IPO i
            LEFT JOIN FETCH i.strengths
            WHERE i.id IN :ids
            """)
    List<IPO> fetchStrengths(@Param("ids") List<UUID> ids);

    @Query("""
            SELECT DISTINCT i
            FROM IPO i
            LEFT JOIN FETCH i.risks
            WHERE i.id IN :ids
            """)
    List<IPO> fetchRisks(@Param("ids") List<UUID> ids);

    @Query("""
            SELECT DISTINCT i
            FROM IPO i
            LEFT JOIN FETCH i.subscriptions
            WHERE i.id IN :ids
            """)
    List<IPO> fetchSubscriptions(@Param("ids") List<UUID> ids);

    @Query("""
            SELECT DISTINCT i
            FROM IPO i
            LEFT JOIN FETCH i.gmp
            WHERE i.id IN :ids
            """)
    List<IPO> fetchGmp(@Param("ids") List<UUID> ids);

    @Query("""
            SELECT DISTINCT i
            FROM IPO i
            LEFT JOIN FETCH i.registrar
            WHERE i.id IN :ids
            """)
    List<IPO> fetchRegistrar(@Param("ids") List<UUID> ids);

    @Query("""
                SELECT i
                FROM IPO i
                LEFT JOIN FETCH i.financials
                WHERE i.id IN :ids
            """)
    List<IPO> fetchFinancials(@Param("ids") List<UUID> ids);

}
