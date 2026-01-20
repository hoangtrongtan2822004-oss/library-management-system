package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 🎯 Unified API Response Wrapper
 * 
 * Mọi API endpoint nên trả về cấu trúc này để Frontend xử lý đồng bộ.
 * 
 * 📌 Lợi ích:
 * - Frontend chỉ cần 1 Interceptor duy nhất để xử lý mọi response
 * - Dễ dàng phân biệt SUCCESS vs ERROR
 * - Timestamp giúp debug và tracking
 * - Generic <T> cho phép wrap bất kỳ kiểu dữ liệu nào
 * 
 * 📌 Ví dụ sử dụng:
 * 
 * // Success response
 * return ApiResponse.success(bookList, "Lấy danh sách sách thành công");
 * 
 * // Error response
 * return ApiResponse.error("Sách không tồn tại", HttpStatus.NOT_FOUND);
 * 
 * // Paginated response
 * return ApiResponse.<Page<Books>>builder()
 *     .status(ResponseStatus.SUCCESS)
 *     .message("Lấy danh sách sách thành công")
 *     .data(pageResult)
 *     .pagination(PaginationMetadata.of(pageResult))
 *     .build();
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * 🎯 Success flag (true/false) - Recommended for Frontend
     * 
     * Easier to check: if (response.success) { ... }
     * vs: if (response.status === "SUCCESS") { ... }
     */
    @Builder.Default
    private boolean success = true;
    
    /**
     * Trạng thái response: SUCCESS hoặc ERROR (legacy field)
     * 
     * ⚠️ Deprecated: Prefer using `success` boolean field
     */
    private ResponseStatus status;
    
    /**
     * Thông báo hiển thị cho user (tiếng Việt hoặc tiếng Anh)
     */
    private String message;
    
    /**
     * 🏷️ Error Code cho Frontend i18n (chỉ có khi success = false)
     * 
     * Format: <RESOURCE>_<STATUS>
     * - USER_404: User không tồn tại
     * - BOOK_409: Sách bị trùng ISBN
     * - LOAN_400: Ngày trả < ngày mượn
     * - TOKEN_EXPIRED: JWT hết hạn
     * 
     * Frontend dùng để hiển thị message đa ngôn ngữ:
     * ```javascript
     * if (response.errorCode === "USER_404") {
     *   showError(t("errors.USER_404")); // i18n translation
     * }
     * ```
     */
    private String errorCode;
    
    /**
     * Dữ liệu thực tế (có thể null nếu là error response)
     */
    private T data;
    
    /**
     * Thông tin phân trang (chỉ có khi data là Page<T>)
     */
    private PaginationMetadata pagination;
    
    /**
     * Timestamp khi tạo response (UTC)
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 🆕 Trace ID cho distributed tracing & debugging
     * 
     * 📌 Lợi ích:
     * - Dễ dàng trace request qua nhiều services (Microservices)
     * - Debug lỗi 500: Tìm log theo traceId thay vì timestamp
     * - Monitoring: Liên kết log từ Frontend → Backend → Database
     * 
     * 💡 Sử dụng:
     * - Frontend gửi kèm X-Trace-ID header hoặc tự generate
     * - Backend auto-generate nếu không có
     * - Ghi vào log mọi nơi: [traceId=abc123] Error occurred...
     */
    @Builder.Default
    private String traceId = java.util.UUID.randomUUID().toString();
    
    /**
     * HTTP Status Code (200, 400, 404, 500...)
     * 
     * ⚠️ Optional: Chỉ cần khi Frontend cần status code chi tiết
     * HTTP status đã có trong ResponseEntity.status()
     */
    private Integer httpStatus;
    
    // ============= BUILDER HELPERS =============
    
    /**
     * Tạo success response với dữ liệu và message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .message(message)
                .data(data)
                .httpStatus(200)
                .build();
    }
    
    /**
     * Tạo success response chỉ với dữ liệu (message mặc định)
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Thành công");
    }
    
    /**
     * Tạo error response với message và HTTP status
     */
    public static <T> ApiResponse<T> error(String message, Integer httpStatus) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.ERROR)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
    
    /**
     * Tạo error response với message, error code và HTTP status
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, Integer httpStatus) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.ERROR)
                .message(message)
                .errorCode(errorCode)
                .httpStatus(httpStatus)
                .build();
    }
    
    /**
     * Enum cho status
     */
    public enum ResponseStatus {
        SUCCESS, ERROR
    }
    
    /**
     * Metadata cho phân trang
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationMetadata {
        private Integer currentPage;
        private Integer pageSize;
        private Long totalElements;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;
        
        /**
         * Tạo PaginationMetadata từ Spring Data Page
         */
        public static PaginationMetadata of(org.springframework.data.domain.Page<?> page) {
            return PaginationMetadata.builder()
                    .currentPage(page.getNumber())
                    .pageSize(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .hasNext(page.hasNext())
                    .hasPrevious(page.hasPrevious())
                    .build();
        }
    }
}
