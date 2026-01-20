package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Feature 9: Gamification - Daily Quests (Nhiệm vụ hàng ngày)
 */
@Entity
@Table(name = "daily_quests")
public class DailyQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "quest_type", nullable = false, length = 50)
    private String questType; // login, search, review, borrow, return

    @Column(name = "target", nullable = false)
    private Integer target = 1; // Số lần cần hoàn thành

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public String getQuestType() { return questType; }
    public void setQuestType(String questType) { this.questType = questType; }

    public Integer getTarget() { return target; }
    public void setTarget(Integer target) { this.target = target; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
