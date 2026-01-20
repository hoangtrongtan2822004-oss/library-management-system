package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.UserQuestProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserQuestProgressRepository extends JpaRepository<UserQuestProgress, Long> {
    List<UserQuestProgress> findByUserIdAndDate(Integer userId, LocalDate date);
    Optional<UserQuestProgress> findByUserIdAndQuestIdAndDate(Integer userId, Long questId, LocalDate date);
}
