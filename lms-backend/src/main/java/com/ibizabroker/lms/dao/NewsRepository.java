package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.isPinned DESC, n.createdAt DESC")
    List<News> findAllPublishedOrderByPinnedAndDate();
    
    @Query("SELECT n FROM News n ORDER BY n.isPinned DESC, n.createdAt DESC")
    List<News> findAllOrderByPinnedAndDate();
    
    @Query("SELECT n FROM News n WHERE n.status = 'DRAFT' AND n.publishedAt <= :now")
    List<News> findDraftNewsReadyToPublish(Instant now);
}
