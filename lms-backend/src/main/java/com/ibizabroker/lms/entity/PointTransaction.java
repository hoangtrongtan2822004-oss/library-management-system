package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Feature 9: Gamification - Point Transactions (Lịch sử giao dịch điểm)
 */
@Entity
@Table(name = "point_transactions", 
    indexes = @Index(name = "idx_user_timestamp", columnList = "user_id, timestamp"))
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "points", nullable = false)
    private Integer points; // Số điểm thay đổi (dương = nhận, âm = mất)

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType; // borrow, return, review, reward, admin, quest

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "reference_id")
    private Long referenceId; // ID tham chiếu (loanId, reviewId, rewardId, etc.)

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter; // Số dư sau giao dịch

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
}
