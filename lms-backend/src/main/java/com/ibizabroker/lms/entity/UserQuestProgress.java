package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Feature 9: Gamification - User Quest Progress (Tiến độ nhiệm vụ người dùng)
 */
@Entity
@Table(name = "user_quest_progress", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "quest_id", "date"}))
public class UserQuestProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "quest_id", nullable = false)
    private Long questId;

    @Column(name = "progress", nullable = false)
    private Integer progress = 0;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "date", nullable = false)
    private LocalDate date = LocalDate.now();

    @Column(name = "points_earned")
    private Integer pointsEarned = 0;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Long getQuestId() { return questId; }
    public void setQuestId(Long questId) { this.questId = questId; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
}
