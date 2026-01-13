package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Tìm các đánh giá đã được phê duyệt cho một cuốn sách
    List<Review> findByBookIdAndApproved(Integer bookId, boolean approved);

    // Tính điểm trung bình của các đánh giá đã được phê duyệt
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId AND r.approved = true")
    Optional<Double> findAverageRatingByBookId(@Param("bookId") Integer bookId);

    // SỬA LỖI TẠI ĐÂY: Đổi tên phương thức để khớp với thuộc tính lồng
    // Tên cũ: existsByBookIdAndUserId
    boolean existsByBookIdAndUser_UserId(Integer bookId, Integer userId);
    
    // Lấy tất cả đánh giá, sắp xếp theo ngày tạo mới nhất
    @EntityGraph(attributePaths = {"book", "user"})
    List<Review> findAllByOrderByCreatedAtDesc();

    // Lấy các đánh giá của một người dùng
    @EntityGraph(attributePaths = {"book", "user"})
    List<Review> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
}