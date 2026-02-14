package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.BarcodeType;
import com.ibizabroker.lms.entity.MemberCard;
import com.ibizabroker.lms.entity.MemberCardStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberCardRepository extends JpaRepository<MemberCard, Long> {

    boolean existsByCardNumber(String cardNumber);

    @Query("SELECT mc FROM MemberCard mc " +
           "JOIN mc.user u " +
           "WHERE (:keyword IS NULL OR " +
           "LOWER(mc.cardNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR mc.status = :status) " +
           "AND (:barcodeType IS NULL OR mc.barcodeType = :barcodeType) " +
           "AND (:fromDate IS NULL OR mc.issuedAt >= :fromDate) " +
           "AND (:toDate IS NULL OR mc.issuedAt <= :toDate)")
    Page<MemberCard> search(
        @Param("keyword") String keyword,
        @Param("status") MemberCardStatus status,
        @Param("barcodeType") BarcodeType barcodeType,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );
}
