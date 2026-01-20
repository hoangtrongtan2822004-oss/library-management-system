package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.DailyQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyQuestRepository extends JpaRepository<DailyQuest, Long> {
    List<DailyQuest> findByActiveTrue();
    DailyQuest findByQuestType(String questType);
}
