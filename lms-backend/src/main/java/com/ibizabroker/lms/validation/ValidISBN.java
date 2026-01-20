package com.ibizabroker.lms.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 📚 Custom Validation: Valid ISBN-10 / ISBN-13
 * 
 * Kiểm tra không chỉ format (10 hoặc 13 chữ số) mà còn **checksum** theo thuật toán chuẩn.
 * 
 * 📌 ISBN-10 Checksum Algorithm:
 * - Nhân mỗi chữ số với trọng số giảm dần từ 10 → 2
 * - Tổng phải chia hết cho 11
 * - Ví dụ: 0-306-40615-2 → (0×10 + 3×9 + 0×8 + 6×7 + 4×6 + 0×5 + 6×4 + 1×3 + 5×2) + 2 = 132 ≡ 0 (mod 11) ✅
 * 
 * 📌 ISBN-13 Checksum Algorithm:
 * - Nhân chữ số chẵn với 1, chữ số lẻ với 3
 * - Tổng phải chia hết cho 10
 * - Ví dụ: 978-0-306-40615-7 → (9×1 + 7×3 + 8×1 + 0×3 + 3×1 + 0×3 + 6×1 + 4×3 + 0×1 + 6×3 + 1×1 + 5×3) + 7 = 100 ≡ 0 (mod 10) ✅
 * 
 * 🎯 Sử dụng:
 * ```java
 * public class BookCreateDto {
 *     @ValidISBN
 *     private String isbn;
 * }
 * ```
 * 
 * ⚠️ Lưu ý:
 * - NULL được coi là valid (dùng @NotNull nếu muốn bắt buộc)
 * - Blank string được coi là valid (dùng @NotBlank nếu muốn bắt buộc)
 * - Tự động remove dấu gạch ngang (-) và spaces trước khi validate
 */
@Documented
@Constraint(validatedBy = ISBNValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidISBN {
    
    /**
     * Error message mặc định
     */
    String message() default "ISBN không hợp lệ (checksum sai)";
    
    /**
     * Validation groups (advanced usage)
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload (advanced usage)
     */
    Class<? extends Payload>[] payload() default {};
}
