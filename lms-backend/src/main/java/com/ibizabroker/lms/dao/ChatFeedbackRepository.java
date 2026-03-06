package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.ChatFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, Long> {

    List<ChatFeedback> findByConversationId(String conversationId);

    List<ChatFeedback> findByUserId(Integer userId);

    /**
     * Count positive vs negative feedback for admin analytics
     */
    @Query("SELECT f.helpful AS helpful, COUNT(f) AS count FROM ChatFeedback f GROUP BY f.helpful")
    List<Map<String, Object>> countByHelpful();

    /**
     * Get recent feedback entries for admin dashboard
     */
    List<ChatFeedback> findTop20ByOrderByCreatedAtDesc();
}
