package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.dto.BookListDto;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.exceptions.NotFoundException;
import com.ibizabroker.lms.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/books")
@CrossOrigin("http://localhost:4200")
@RequiredArgsConstructor
@Tag(name = "Public Books", description = "API công khai cho danh sách sách (không cần đăng nhập)")
public class PublicBooksController {

    private final BookService bookService;

    @GetMapping
    @Operation(
        summary = "Danh sách sách có phân trang",
        description = "Lấy danh sách sách với filter theo tên, thể loại, tình trạng. Trả về Slim DTO (giảm 70% kích thước so với full entity)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    public ResponseEntity<ApiResponse<Page<BookListDto>>> listBooks(
            @Parameter(description = "Tìm kiếm theo tên sách hoặc tác giả") @RequestParam(required = false) String search,
            @Parameter(description = "Lọc theo thể loại") @RequestParam(required = false) String genre,
            @Parameter(description = "Chỉ hiển sách còn hàng") @RequestParam(required = false) Boolean availableOnly,
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang") @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Books> booksPage = bookService.findBooksWithFilters(search, genre, availableOnly, pageable);
        
        // Chuyển Entity sang Slim DTO để giảm kích thước response
        Page<BookListDto> dtoPage = booksPage.map(BookListDto::fromEntity);
        
        ApiResponse<Page<BookListDto>> response = ApiResponse.<Page<BookListDto>>builder()
                .status(ApiResponse.ResponseStatus.SUCCESS)
                .message("Lấy danh sách sách thành công")
                .data(dtoPage)
                .pagination(ApiResponse.PaginationMetadata.of(dtoPage))
                .httpStatus(200)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/newest")
    @Operation(summary = "Sách mới nhất", description = "Lấy 10 cuốn sách mới nhất trong thư viện")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    public ResponseEntity<ApiResponse<List<BookListDto>>> getNewestBooks() {
        Pageable top10 = PageRequest.of(0, 10);
        List<Books> books = bookService.getNewestBooks(top10);
        
        // Convert to Slim DTO
        List<BookListDto> dtos = books.stream()
                .map(BookListDto::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtos, "Lấy danh sách sách mới thành công"));
    }

    // ✅ BỔ SUNG API NÀY ĐỂ TÍNH NĂNG QUÉT QR HOẠT ĐỘNG
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sách", description = "Lấy thông tin chi tiết của một cuốn sách (full entity)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sách không tồn tại")
    public ResponseEntity<ApiResponse<BookListDto>> getBookById(@PathVariable Integer id) {
        try {
            Books book = bookService.getBookById(id);
            BookListDto dto = BookListDto.fromEntity(book);
            return ResponseEntity.ok(ApiResponse.success(dto, "Lấy thông tin sách thành công"));
        } catch (NotFoundException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("Sách không tồn tại", 404));
        }
    }
}
