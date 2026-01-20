package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional; // Thêm import này

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUser(Users user);
    
    // Sorting methods
    List<Wishlist> findByUserOrderByCreatedAtDesc(Users user); // Mới nhất trước
    List<Wishlist> findByUserOrderByBook_NameAsc(Users user); // Sắp xếp theo tên sách A-Z (book.name)
    
    boolean existsByUserAndBook(Users user, Books book);
    
    Optional<Wishlist> findByUserAndBook(Users user, Books book);

    // Bổ sung @Transactional để cho phép thực hiện thao tác xóa dữ liệu
    @Transactional
    void deleteByUserAndBook(Users user, Books book);
}