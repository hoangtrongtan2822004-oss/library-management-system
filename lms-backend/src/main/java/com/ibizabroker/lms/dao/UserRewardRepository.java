package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, Long> {
    List<UserReward> findByUserId(Integer userId);
    List<UserReward> findByUserIdAndUsedFalse(Integer userId);
    long countByRewardId(Long rewardId);
}
