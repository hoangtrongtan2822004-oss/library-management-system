package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.dto.BookCreateDto; // Import DTO
import com.ibizabroker.lms.dto.BookUpdateDto; // Import DTO
import com.ibizabroker.lms.entity.Author;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Category;
import com.ibizabroker.lms.service.BookService; // Import service
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor; // Thêm
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/admin/books") // <--- THÊM /api
@PreAuthorize("hasRole('ADMIN')") // Bảo vệ tất cả các endpoint trong controller này
@RequiredArgsConstructor // Thêm
@Tag(name = "Admin Books", description = "Quản lý sách (Admin only)")
public class BooksController {

    private final BookService bookService; // Chỉ inject service

    // GET /admin/books đã bị xóa, vì admin sẽ dùng
    // GET /public/books?availableOnly=false (đã được phân trang)

    // API này giữ lại (hoặc chuyển sang PublicBooksController)
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết sách", description = "Lấy thông tin đầy đủ của một cuốn sách theo ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sách không tồn tại")
    public ResponseEntity<ApiResponse<Books>> getBookById(@PathVariable Integer id) {
        Books book = bookService.getBookById(id);
        return ResponseEntity.ok(ApiResponse.success(book, "Lấy thông tin sách thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo sách mới", description = "Thêm một cuốn sách mới vào thư viện")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo sách thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    public ResponseEntity<ApiResponse<Books>> createBook(@Valid @RequestBody BookCreateDto bookDto) { // Dùng DTO
        Books created = bookService.createBook(bookDto);
        return ResponseEntity.ok(ApiResponse.success(created, "Tạo sách thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sách", description = "Cập nhật thông tin của một cuốn sách hiện có")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sách không tồn tại")
    public ResponseEntity<ApiResponse<Books>> updateBook(@PathVariable Integer id, @Valid @RequestBody BookUpdateDto bookDto) { // Dùng DTO
        Books updatedBook = bookService.updateBook(id, bookDto);
        return ResponseEntity.ok(ApiResponse.success(updatedBook, "Cập nhật sách thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa sách", description = "Xóa một cuốn sách khỏi thư viện")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sách không tồn tại")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteBook(@PathVariable Integer id) {
        bookService.deleteBook(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", Boolean.TRUE), "Xóa sách thành công"));
    }

    // --- API mới để hỗ trợ form ---
    
    @GetMapping("/authors")
    @Operation(summary = "Lấy danh sách tác giả", description = "Lấy tất cả tác giả để hiển thị trong dropdown form")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    public ResponseEntity<ApiResponse<List<Author>>> getAllAuthors() {
        List<Author> authors = bookService.getAllAuthors();
        return ResponseEntity.ok(ApiResponse.success(authors, "Lấy danh sách tác giả thành công"));
    }

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh sách thể loại", description = "Lấy tất cả thể loại/danh mục để hiển thị trong dropdown form")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        List<Category> categories = bookService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Lấy danh sách thể loại thành công"));
    }
}
