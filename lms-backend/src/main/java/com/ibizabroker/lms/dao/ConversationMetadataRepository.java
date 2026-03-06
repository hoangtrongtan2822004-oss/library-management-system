package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.ConversationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMetadataRepository extends JpaRepository<ConversationMetadata, String> {

    List<ConversationMetadata> findByUserId(Integer userId);

    Optional<ConversationMetadata> findByConversationIdAndUserId(String conversationId, Integer userId);
}
