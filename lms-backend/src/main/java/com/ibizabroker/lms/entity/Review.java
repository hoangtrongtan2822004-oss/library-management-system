package com.ibizabroker.lms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = {
    // Đảm bảo mỗi người dùng chỉ đánh giá một cuốn sách một lần
    @UniqueConstraint(columnNames = {"book_id", "user_id"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Books book;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @NotNull
    @Min(value = 1, message = "Điểm số phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm số phải từ 1 đến 5")
    @Column(nullable = false)
    private Integer rating; // Điểm số từ 1-5

    @Column(columnDefinition = "TEXT")
    private String comment; // Bình luận (tùy chọn)

    @Column(columnDefinition = "TEXT")
    private String images; // JSON array chứa URLs của ảnh (tùy chọn)

    @Column(nullable = false)
    private boolean approved = false; // Trạng thái phê duyệt, mặc định là false

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    private String adminReply;

    @Column(name = "admin_reply_date")
    private LocalDateTime adminReplyDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ReviewLike> likes = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<ReviewComment> comments = new java.util.ArrayList<>();

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Books getBook() { return book; }
    public void setBook(Books book) { this.book = book; }
    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    public LocalDateTime getAdminReplyDate() { return adminReplyDate; }
    public void setAdminReplyDate(LocalDateTime adminReplyDate) { this.adminReplyDate = adminReplyDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public java.util.List<ReviewLike> getLikes() { return likes; }
    public void setLikes(java.util.List<ReviewLike> likes) { this.likes = likes; }
    public java.util.List<ReviewComment> getComments() { return comments; }
    public void setComments(java.util.List<ReviewComment> comments) { this.comments = comments; }
}