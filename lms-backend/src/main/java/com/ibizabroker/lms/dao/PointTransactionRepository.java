package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    List<PointTransaction> findByUserIdOrderByTimestampDesc(Integer userId);
    
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.userId = :userId AND pt.timestamp >= :startDate ORDER BY pt.timestamp DESC")
    List<PointTransaction> findByUserIdAndTimestampAfter(@Param("userId") Integer userId, @Param("startDate") LocalDateTime startDate);
}
