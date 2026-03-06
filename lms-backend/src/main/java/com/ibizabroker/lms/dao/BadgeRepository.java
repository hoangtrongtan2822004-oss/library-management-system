package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByCode(String code);

    boolean existsByCode(String code);
    
    List<Badge> findByIsActiveTrue();
    
    List<Badge> findByCategory(String category);
    
    List<Badge> findByCategoryAndIsActiveTrue(String category);
}
