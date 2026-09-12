package com.virtu_stock.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.virtu_stock.Models.IPO;

public interface IPORepository extends JpaRepository<IPO, UUID>, JpaSpecificationExecutor<IPO> {
    public boolean existsByIpoAlertId(String ipoAlertId);

    public List<IPO> findAllByOrderByEndDateDesc();

    public List<IPO> findByListingDateLessThanEqual(LocalDate date);

    long countByStartDateBetween(LocalDate start, LocalDate end);

    public List<IPO> findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCaseOrderByName(String name, String symbol);

    @Query("""
                SELECT MONTH(i.startDate), COUNT(i)
                FROM IPO i
                WHERE YEAR(i.startDate) = :year
                GROUP BY MONTH(i.startDate)
            """)
    List<Object[]> countIpoByMonthAndYear(@Param("year") int year);

   
}
