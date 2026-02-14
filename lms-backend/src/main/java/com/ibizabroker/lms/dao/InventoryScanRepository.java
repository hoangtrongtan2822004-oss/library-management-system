package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.InventoryScan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryScanRepository extends JpaRepository<InventoryScan, Long> {

    boolean existsBySessionIdAndBookId(Long sessionId, Integer bookId);

    List<InventoryScan> findBySessionId(Long sessionId);

    List<InventoryScan> findBySessionIdAndUnknownTrue(Long sessionId);
}
