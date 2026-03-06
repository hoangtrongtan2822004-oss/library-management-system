package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Find all messages in a conversation
     */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    
    /**
     * Find all conversations for a user (grouped so MAX in ORDER BY is valid)
     */
    @Query("SELECT cm.conversationId FROM ChatMessage cm WHERE cm.userId = :userId GROUP BY cm.conversationId ORDER BY MAX(cm.createdAt) DESC")
    List<String> findUserConversationIds(@Param("userId") Integer userId);
    
    /**
     * Find recent messages for context (last 5 messages)
     */
    @Query(value = "SELECT * FROM chat_messages WHERE conversation_id = :conversationId ORDER BY created_at DESC LIMIT 5", 
           nativeQuery = true)
    List<ChatMessage> findRecentMessages(@Param("conversationId") String conversationId);

    /**
     * Delete all messages in a conversation belonging to a specific user (ownership check)
     */
    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.userId = :userId")
    void deleteByConversationIdAndUserId(@Param("conversationId") String conversationId, @Param("userId") Integer userId);
}
