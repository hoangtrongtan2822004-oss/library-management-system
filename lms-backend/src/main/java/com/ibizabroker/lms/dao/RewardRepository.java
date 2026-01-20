package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
    List<Reward> findByAvailableTrue();
    List<Reward> findByCategory(String category);
}
