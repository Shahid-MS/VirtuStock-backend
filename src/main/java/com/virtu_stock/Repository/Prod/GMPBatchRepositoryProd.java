package com.virtu_stock.Repository.Prod;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.virtu_stock.Repository.GMPBatchRepository;
import com.virtu_stock.Repository.GMPInsert;
import com.virtu_stock.Repository.GMPUpdate;

import lombok.RequiredArgsConstructor;

@Repository
@Profile("prod")
@RequiredArgsConstructor
public class GMPBatchRepositoryProd implements GMPBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void insertGmpBatch(List<GMPInsert> gmps) {

        String sql = """
                INSERT INTO gmp
                    (ipo_id, gmp, gmp_date, last_updated)
                VALUES
                    (?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(
                sql,
                gmps,
                gmps.size(),
                (ps, gmp) -> {
                    ps.setObject(1, gmp.ipoId());
                    ps.setDouble(2, gmp.gmp());
                    ps.setObject(3, gmp.gmpDate());
                    ps.setObject(4, gmp.lastUpdated());
                });
    }

    @Override
    public void updateGmpBatch(List<GMPUpdate> gmps) {

        String sql = """
                UPDATE gmp
                SET gmp = ?, last_updated = ?
                WHERE ipo_id = ?
                  AND gmp_date = ?
                """;

        jdbcTemplate.batchUpdate(
                sql,
                gmps,
                gmps.size(),
                (ps, gmp) -> {
                    ps.setDouble(1, gmp.gmp());
                    ps.setObject(2, gmp.lastUpdated());
                    ps.setObject(3, gmp.ipoId());
                    ps.setObject(4, gmp.gmpDate());
                });
    }
}