package com.virtu_stock.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.virtu_stock.Models.IPOFinancial;

public interface IPOFinancialRepository extends JpaRepository<IPOFinancial, Long> {

    Optional<IPOFinancial> findByIpoId(UUID ipoId);

    boolean existsByIpoId(UUID ipoId);
}
