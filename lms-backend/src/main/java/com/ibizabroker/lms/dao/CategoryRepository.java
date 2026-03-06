package com.ibizabroker.lms.dao;

import com.ibizabroker.lms.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // Tìm một tập hợp các thể loại bằng ID
    Set<Category> findByIdIn(Set<Integer> ids);

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Integer id);

    // Đếm số sách trong một danh mục
    @Query("SELECT COUNT(b) FROM Books b JOIN b.categories c WHERE c.id = :categoryId")
    int countBooksByCategoryId(@Param("categoryId") Integer categoryId);

    // Cập nhật tất cả sách từ danh mục nguồn sang danh mục đích
    @Modifying
    @Query(value = "UPDATE book_categories SET category_id = :toId WHERE category_id = :fromId AND book_id NOT IN (SELECT book_id FROM (SELECT book_id FROM book_categories WHERE category_id = :toId) AS already_has)", nativeQuery = true)
    void migrateBooksToCategory(@Param("fromId") Integer fromId, @Param("toId") Integer toId);

    // Xóa tất cả liên kết sách của một danh mục
    @Modifying
    @Query(value = "DELETE FROM book_categories WHERE category_id = :categoryId", nativeQuery = true)
    void deleteAllBookLinks(@Param("categoryId") Integer categoryId);
}