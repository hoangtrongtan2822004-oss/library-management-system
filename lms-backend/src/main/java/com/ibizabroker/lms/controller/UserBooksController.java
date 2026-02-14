package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.dto.BookListDto;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.service.BookService;
import com.ibizabroker.lms.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/books")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER','ADMIN')")
@Tag(name = "User Books", description = "Gợi ý sách cho người dùng")
public class UserBooksController {

    private final BookService bookService;
    private final UsersRepository usersRepository;

    @GetMapping("/recommendations")
    @Operation(summary = "Sách gợi ý", description = "Gợi ý sách dựa trên lịch sử mượn")
    public ResponseEntity<ApiResponse<List<BookListDto>>> getRecommendations(
            @RequestParam(defaultValue = "6") int size) {
        Integer userId = SecurityUtils.getCurrentUserId(usersRepository)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập"));

        List<Books> books = bookService.getRecommendationsForUser(userId, size);
        List<BookListDto> dtos = books.stream()
                .map(BookListDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "Lấy danh sách sách gợi ý thành công"));
    }
}
