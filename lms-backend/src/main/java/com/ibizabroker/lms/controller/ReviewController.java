package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.ReviewRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dao.ReviewLikeRepository;
import com.ibizabroker.lms.dao.ReviewCommentRepository;
import com.ibizabroker.lms.dto.ReviewDto;
import com.ibizabroker.lms.dto.ReviewCommentDto;
import com.ibizabroker.lms.dto.ReviewCommentRequest;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Review;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.entity.ReviewLike;
import com.ibizabroker.lms.entity.ReviewComment;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.AiTaggingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ObjectMapper objectMapper;
    private final AiTaggingService aiTaggingService;

    public ReviewController(ReviewRepository reviewRepository, BooksRepository booksRepository,
                          UsersRepository usersRepository, ReviewLikeRepository reviewLikeRepository,
                          ReviewCommentRepository reviewCommentRepository, ObjectMapper objectMapper,
                          AiTaggingService aiTaggingService) {
        this.reviewRepository = reviewRepository;
        this.booksRepository = booksRepository;
        this.usersRepository = usersRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.reviewCommentRepository = reviewCommentRepository;
        this.objectMapper = objectMapper;
        this.aiTaggingService = aiTaggingService;
    }

    // --- API CHO NGƯỜI DÙNG ---

    @PostMapping("/api/books/{bookId}/reviews")
    @PreAuthorize("hasRole('USER')")
    @SuppressWarnings("null") // Spring Data findById(Integer) lacks @NonNull; @PathVariable is always non-null
    public ResponseEntity<?> addReview(@PathVariable Integer bookId,
                                       @Valid @RequestBody ReviewDto reviewDto,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));

        if (reviewRepository.existsByBookIdAndUser_UserId(bookId, currentUser.getUserId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Bạn đã đánh giá sách này rồi."));
        }

        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sách với ID: " + bookId));

        Review review = new Review();
        review.setBook(book);
        review.setUser(currentUser);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        
        // Lưu images dạng JSON
        if (reviewDto.getImages() != null && !reviewDto.getImages().isEmpty()) {
            try {
                review.setImages(objectMapper.writeValueAsString(reviewDto.getImages()));
            } catch (JsonProcessingException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Lỗi xử lý ảnh: " + e.getMessage()));
            }
        }

        // AI moderation: auto-approve if content is acceptable, otherwise mark for manual review
        boolean isAcceptable = aiTaggingService.moderateReview(reviewDto.getComment());
        review.setApproved(isAcceptable);

        Review savedReview = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(savedReview, null));
    }

    @GetMapping("/api/books/{bookId}/reviews")
    public ResponseEntity<Map<String, Object>> getReviewsForBook(
            @PathVariable Integer bookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Integer currentUserId = null;
        if (userDetails != null) {
            Users user = usersRepository.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null) currentUserId = user.getUserId();
        }
        
        Integer finalUserId = currentUserId;
        List<ReviewDto> reviews = reviewRepository.findByBookIdAndApproved(bookId, true)
                .stream()
                .map(r -> toDto(r, finalUserId))
                .collect(Collectors.toList());
        
        Double averageRating = reviewRepository.findAverageRatingByBookId(bookId).orElse(0.0);

        return ResponseEntity.ok(Map.of(
            "reviews", reviews,
            "averageRating", averageRating
        ));
    }

    @GetMapping("/api/my/reviews")
    @PreAuthorize("hasRole('USER')")
    public List<ReviewDto> getMyReviews(@AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));
        return reviewRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUser.getUserId())
                .stream()
                .map(r -> toDto(r, currentUser.getUserId()))
                .collect(Collectors.toList());
    }

    @PutMapping("/api/my/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewDto> updateMyReview(@PathVariable Integer reviewId,
                            @Valid @RequestBody ReviewDto reviewDto,
                            @AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));
        @SuppressWarnings("null")
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá"));
        if (!review.getUser().getUserId().equals(currentUser.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setApproved(false); // chỉnh sửa cần duyệt lại (tuỳ chính sách)
        Review saved = reviewRepository.save(review);
        return ResponseEntity.ok(toDto(saved, currentUser.getUserId()));
    }

    @DeleteMapping("/api/my/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteMyReview(@PathVariable Integer reviewId,
                           @AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));
        @SuppressWarnings("null")
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá"));
        if (!review.getUser().getUserId().equals(currentUser.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        reviewRepository.delete(review);
        return ResponseEntity.noContent().build();
    }

    // --- API CHO ADMIN ---

    @GetMapping("/api/admin/reviews") // <-- Đảm bảo endpoint này tồn tại
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReviewDto> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> toDto(r, null))
                .collect(Collectors.toList());
    }

    @GetMapping("/api/books/{bookId}/reviews/check")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> hasUserReviewed(
            @PathVariable Integer bookId, 
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));
            
        boolean hasReviewed = reviewRepository.existsByBookIdAndUser_UserId(bookId, currentUser.getUserId());
    
        return ResponseEntity.ok(Map.of("hasReviewed", hasReviewed));
    }


    @PutMapping("/api/admin/reviews/{reviewId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewDto> approveReview(@PathVariable Integer reviewId) {
        @SuppressWarnings("null")
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));
        
        review.setApproved(true);
        Review updatedReview = reviewRepository.save(review);
        return ResponseEntity.ok(toDto(updatedReview, null));
    }

    @SuppressWarnings("null")
    @DeleteMapping("/api/admin/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Integer reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            return ResponseEntity.notFound().build();
        }
        reviewRepository.deleteById(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/reviews/{reviewId}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewDto> addAdminReply(
            @PathVariable Integer reviewId,
            @RequestBody Map<String, String> body) {
        @SuppressWarnings("null")
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đánh giá với ID: " + reviewId));
        String replyText = body.getOrDefault("replyText", "").trim();
        if (replyText.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        review.setAdminReply(replyText);
        review.setAdminReplyDate(java.time.LocalDateTime.now());
        Review saved = reviewRepository.save(review);
        return ResponseEntity.ok(toDto(saved, null));
    }

    @PostMapping("/api/admin/reviews/bulk-approve")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("null") // Spring Data findAllById/saveAll lack @NonNull on Iterable param; List.of() is non-null
    public ResponseEntity<Map<String, Object>> bulkApprove(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> ids = body.getOrDefault("reviewIds", List.of());
        List<Review> reviews = reviewRepository.findAllById(ids);
        reviews.forEach(r -> r.setApproved(true));
        reviewRepository.saveAll(reviews);
        return ResponseEntity.ok(Map.of("updated", reviews.size()));
    }

    @PostMapping("/api/admin/reviews/bulk-delete")
    @PreAuthorize("hasRole('ADMIN')")
    @SuppressWarnings("null") // Spring Data deleteAllById lacks @NonNull on Iterable param; List.of() is non-null
    public ResponseEntity<Map<String, Object>> bulkDelete(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> ids = body.getOrDefault("reviewIds", List.of());
        reviewRepository.deleteAllById(ids);
        return ResponseEntity.ok(Map.of("deleted", ids.size()));
    }
    
    // --- API LIKE/UNLIKE REVIEW ---
    
    @PostMapping("/api/reviews/{reviewId}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> likeReview(
            @PathVariable Integer reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));
        
        @SuppressWarnings("null")
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review không tồn tại"));
        
        // Kiểm tra đã like chưa
        if (reviewLikeRepository.existsByReview_IdAndUser_UserId(reviewId, currentUser.getUserId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Bạn đã like review này rồi"));
        }
        
        ReviewLike like = new ReviewLike();
        like.setReview(review);
        like.setUser(currentUser);
        reviewLikeRepository.save(like);
        
        Long likesCount = reviewLikeRepository.countByReviewId(reviewId);
        return ResponseEntity.ok(Map.of("likesCount", likesCount, "liked", true));
    }
    
    @SuppressWarnings("null")
    @DeleteMapping("/api/reviews/{reviewId}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> unlikeReview(
            @PathVariable Integer reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));
        
        @SuppressWarnings("null")
        ReviewLike like = reviewLikeRepository.findByReview_IdAndUser_UserId(reviewId, currentUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Chưa like review này"));
        
        reviewLikeRepository.delete(like);
        
        Long likesCount = reviewLikeRepository.countByReviewId(reviewId);
        return ResponseEntity.ok(Map.of("likesCount", likesCount, "liked", false));
    }
    
    // --- API COMMENTS ON REVIEW ---
    
    @GetMapping("/api/reviews/{reviewId}/comments")
    public ResponseEntity<List<ReviewCommentDto>> getCommentsForReview(@PathVariable Integer reviewId) {
        List<ReviewComment> comments = reviewCommentRepository.findByReview_IdOrderByCreatedAtAsc(reviewId);
        List<ReviewCommentDto> dtos = comments.stream()
                .map(this::commentToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/api/reviews/{reviewId}/comments")
    @PreAuthorize("hasRole('USER')")
        public ResponseEntity<ReviewCommentDto> addComment(
            @PathVariable Integer reviewId,
            @Valid @RequestBody ReviewCommentRequest commentDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));
        
        @SuppressWarnings("null")
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review không tồn tại"));
        
        ReviewComment comment = new ReviewComment();
        comment.setReview(review);
        comment.setUser(currentUser);
        comment.setContent(commentDto.getContent());
        
        ReviewComment saved = reviewCommentRepository.save(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentToDto(saved));
    }
    
    @DeleteMapping("/api/reviews/comments/{commentId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Integer commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Users currentUser = usersRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("Người dùng không hợp lệ"));
        
        @SuppressWarnings("null")
        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment không tồn tại"));
        
        if (!comment.getUser().getUserId().equals(currentUser.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        reviewCommentRepository.delete(comment);
        return ResponseEntity.noContent().build();
    }
    
    // --- HÀM TIỆN ÍCH ---

    private ReviewDto toDto(Review review, Integer currentUserId) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setBookId(review.getBook().getId());
        dto.setBookName(review.getBook().getName());
        dto.setUserId(review.getUser().getUserId());
        dto.setUserName(review.getUser().getName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setApproved(review.isApproved());
        dto.setCreatedAt(review.getCreatedAt().toString());
        
        // Parse images từ JSON
        if (review.getImages() != null) {
            try {
                dto.setImages(objectMapper.readValue(review.getImages(), new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException e) {
                dto.setImages(new ArrayList<>());
            }
        }
        
        // Đếm likes & comments
        dto.setLikesCount(reviewLikeRepository.countByReviewId(review.getId()));
        dto.setCommentsCount(reviewCommentRepository.countByReview_Id(review.getId()));
        
        // Kiểm tra user hiện tại đã like chưa
        if (currentUserId != null) {
            dto.setCurrentUserLiked(reviewLikeRepository.existsByReview_IdAndUser_UserId(review.getId(), currentUserId));
        }

        // Ảnh bìa sách
        dto.setBookCoverUrl(review.getBook().getCoverUrl());

        // Admin reply
        if (review.getAdminReply() != null) {
            dto.setAdminReply(review.getAdminReply());
        }
        if (review.getAdminReplyDate() != null) {
            dto.setAdminReplyDate(review.getAdminReplyDate().toString());
        }
        
        return dto;
    }
    
    private ReviewCommentDto commentToDto(ReviewComment comment) {
        ReviewCommentDto dto = new ReviewCommentDto();
        dto.setId(comment.getId());
        dto.setReviewId(comment.getReview().getId());
        dto.setUserId(comment.getUser().getUserId());
        dto.setUserName(comment.getUser().getName());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt().toString());
        return dto;
    }
}
