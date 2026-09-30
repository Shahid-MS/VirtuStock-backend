package com.virtu_stock.Repository;

import java.util.List;

public interface GMPBatchRepository {

    void insertGmpBatch(List<GMPInsert> gmps);
    void updateGmpBatch(List<GMPUpdate> gmps);
}