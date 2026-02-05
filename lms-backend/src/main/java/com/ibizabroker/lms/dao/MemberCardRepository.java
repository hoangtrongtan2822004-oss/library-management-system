package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.MemberCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface MemberCardRepository extends JpaRepository<MemberCard, Long>, JpaSpecificationExecutor<MemberCard> {
    Optional<MemberCard> findByCardNumber(String cardNumber);

    Optional<MemberCard> findFirstByUserUserIdOrderByIssuedAtDesc(Integer userId);
}
