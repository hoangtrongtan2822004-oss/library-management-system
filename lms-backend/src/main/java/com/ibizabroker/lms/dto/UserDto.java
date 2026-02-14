package com.ibizabroker.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 👤 User DTO - Response only
 * 
 * ⚠️ KHÔNG BAO GIỜ dùng DTO này cho Create/Update request
 * → Dùng UserCreateRequest/UserUpdateRequest để tránh lộ dữ liệu nhạy cảm
 * 
 * 📌 Security Note:
 * - Không bao giờ expose password trong response
 * - Không expose email nếu không cần thiết
 * - Chỉ trả về roles dạng List<String> thay vì full Role entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Integer userId;
    private String name;
    private String username;
    private Boolean active;
    private List<String> roles;
}
