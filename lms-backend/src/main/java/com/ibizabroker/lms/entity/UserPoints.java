package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Feature 9: Gamification - Điểm thưởng người dùng
 */
@Entity
@Table(name = "user_points")
public class UserPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "current_level", nullable = false)
    private Integer currentLevel = 1;

    @Column(name = "books_borrowed_count", nullable = false)
    private Integer booksBorrowedCount = 0;

    @Column(name = "books_returned_on_time", nullable = false)
    private Integer booksReturnedOnTime = 0;

    @Column(name = "reviews_written", nullable = false)
    private Integer reviewsWritten = 0;

    @Column(name = "streak_days", nullable = false)
    private Integer streakDays = 0; // Số ngày liên tục hoạt động

    @Column(name = "streak_freeze_count", nullable = false)
    private Integer streakFreezeCount = 0; // Số lần freeze streak đã mua

    @Column(name = "last_activity_date")
    private LocalDateTime lastActivityDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getTotalPoints() { return totalPoints; }
    public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }

    public Integer getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Integer currentLevel) { this.currentLevel = currentLevel; }

    public Integer getBooksBorrowedCount() { return booksBorrowedCount; }
    public void setBooksBorrowedCount(Integer booksBorrowedCount) { this.booksBorrowedCount = booksBorrowedCount; }

    public Integer getBooksReturnedOnTime() { return booksReturnedOnTime; }
    public void setBooksReturnedOnTime(Integer booksReturnedOnTime) { this.booksReturnedOnTime = booksReturnedOnTime; }

    public Integer getReviewsWritten() { return reviewsWritten; }
    public void setReviewsWritten(Integer reviewsWritten) { this.reviewsWritten = reviewsWritten; }

    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }

    public Integer getStreakFreezeCount() { return streakFreezeCount; }
    public void setStreakFreezeCount(Integer streakFreezeCount) { this.streakFreezeCount = streakFreezeCount; }

    public LocalDateTime getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(LocalDateTime lastActivityDate) { this.lastActivityDate = lastActivityDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public void addPoints(int points) {
        this.totalPoints += points;
        this.updatedAt = LocalDateTime.now();
        updateLevel();
    }

    private void updateLevel() {
        // Level thresholds: 1=0, 2=100, 3=300, 4=600, 5=1000, 6=1500...
        if (totalPoints >= 1500) currentLevel = 6;
        else if (totalPoints >= 1000) currentLevel = 5;
        else if (totalPoints >= 600) currentLevel = 4;
        else if (totalPoints >= 300) currentLevel = 3;
        else if (totalPoints >= 100) currentLevel = 2;
        else currentLevel = 1;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
