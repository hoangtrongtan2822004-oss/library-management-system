package com.ibizabroker.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDto {
    private Long wishlistId;
    private Integer bookId;
    private String bookName;
    private String bookIsbn;
    private String coverUrl;
    private String authorName; // Tác giả chính
    private String categoryName; // Thể loại chính
    private Integer availableCopies; // Số sách còn có thể mượn
    private String notes; // Ghi chú cá nhân của user
    private LocalDateTime addedDate; // Ngày thêm vào wishlist
    private LocalDateTime updatedDate; // Ngày cập nhật ghi chú
}
