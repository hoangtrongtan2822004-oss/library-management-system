package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.LoanRepository;
import com.ibizabroker.lms.dto.UserCreateDto;
import com.ibizabroker.lms.dto.UserDto;
import com.ibizabroker.lms.dto.UserUpdateDto;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/admin") // Đổi root path để chứa cả users và dashboard
@PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final LoanRepository loanRepository; // Inject LoanRepository để lấy stats

    // === USER MANAGEMENT ENDPOINTS (/api/admin/users) ===

    @GetMapping("/users")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody UserCreateDto userCreateDto) {
        return userService.createUser(userCreateDto);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Integer id) {
        Users user = userService.getUserById(id);
        UserDto dto = userService.mapToUserDto(user);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Integer id, @RequestBody UserUpdateDto userDetails) {
        Users updatedUser = userService.updateUser(id, userDetails);
        UserDto dto = userService.mapToUserDto(updatedUser);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("deleted", Boolean.TRUE));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Integer id) {
        Map<String, String> response = userService.resetPassword(id);
        return ResponseEntity.ok(response);
    }

    // === DASHBOARD CHART DATA (/api/admin/dashboard/chart-data) ===

    @GetMapping("/dashboard/chart-data")
    public ResponseEntity<Map<String, Object>> getChartData() {
        Map<String, Object> response = new HashMap<>();
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.with(TemporalAdjusters.firstDayOfYear());
        LocalDate endOfYear = now.with(TemporalAdjusters.lastDayOfYear());

        // 1. Biểu đồ đường: Mượn sách theo tháng
        List<Map<String, Object>> loanStats = loanRepository.findLoanCountsByMonth(startOfYear, endOfYear);
        long[] monthlyData = new long[12];
        for (Map<String, Object> row : loanStats) {
            // row keys: "month" (YYYY-MM as String), "count"
            Object monthObj = row.get("month");
            Object countObj = row.get("count");
            if (monthObj != null && countObj != null) {
                String monthStr = monthObj.toString();
                long count = ((Number) countObj).longValue();
                try {
                    int month = java.time.YearMonth.parse(monthStr).getMonthValue();
                    if (month >= 1 && month <= 12) {
                        monthlyData[month - 1] = count;
                    }
                } catch (Exception ignored) {
                    // Skip malformed month strings instead of failing the dashboard
                }
            }
        }
        response.put("monthlyLoans", monthlyData);

        // 2. Biểu đồ tròn: Trạng thái đơn mượn
        List<Object[]> statusStats = loanRepository.countLoansByStatus();
        Map<String, Long> statusData = new HashMap<>();
        // Init defaults
        statusData.put("ACTIVE", 0L);
        statusData.put("RETURNED", 0L);
        statusData.put("OVERDUE", 0L);
        
        for (Object[] row : statusStats) {
            String status = row[0].toString();
            long count = ((Number) row[1]).longValue();
            statusData.put(status, count);
        }
        response.put("statusDistribution", statusData);

        // 3. Tổng tiền phạt (tất cả) và tổng tiền phạt chưa thanh toán
        BigDecimal totalFines = loanRepository.getTotalFines();
        BigDecimal totalUnpaidFines = loanRepository.getTotalUnpaidFines();
        response.put("totalFines", totalFines);
        response.put("totalUnpaidFines", totalUnpaidFines);

        return ResponseEntity.ok(response);
    }
}