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

}
