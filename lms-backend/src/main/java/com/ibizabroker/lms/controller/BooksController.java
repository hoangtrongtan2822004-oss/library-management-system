package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ApiResponse;
import com.ibizabroker.lms.dto.BookCreateDto;
import com.ibizabroker.lms.dto.BookListDto;
import com.ibizabroker.lms.dto.BookUpdateDto;
import com.ibizabroker.lms.entity.Author;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Category;
import com.ibizabroker.lms.service.AiTaggingService;
import com.ibizabroker.lms.service.BookService;
import com.ibizabroker.lms.service.RagService;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private final BookService bookService;
    private final RagService ragService;
    private final AiTaggingService aiTaggingService;

    // GET /admin/books đã bị xóa, vì admin sẽ dùng
    // GET /public/books?availableOnly=false (đã được phân trang)

    // API này giữ lại (hoặc chuyển sang PublicBooksController)
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết sách", description = "Lấy thông tin đầy đủ của một cuốn sách theo ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sách không tồn tại")
    public ResponseEntity<ApiResponse<BookListDto>> getBookById(@PathVariable Integer id) {
        Books book = bookService.getBookById(id);
        return ResponseEntity.ok(ApiResponse.success(BookListDto.fromEntity(book), "Lấy thông tin sách thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo sách mới", description = "Thêm một cuốn sách mới vào thư viện")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo sách thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    public ResponseEntity<ApiResponse<BookListDto>> createBook(@Valid @RequestBody BookCreateDto bookDto) { // Dùng DTO
        Books created = bookService.createBook(bookDto);
        return ResponseEntity.ok(ApiResponse.success(BookListDto.fromEntity(created), "Tạo sách thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật sách", description = "Cập nhật thông tin của một cuốn sách hiện có")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sách không tồn tại")
    public ResponseEntity<ApiResponse<BookListDto>> updateBook(@PathVariable Integer id, @Valid @RequestBody BookUpdateDto bookDto) { // Dùng DTO
        Books updatedBook = bookService.updateBook(id, bookDto);
        return ResponseEntity.ok(ApiResponse.success(BookListDto.fromEntity(updatedBook), "Cập nhật sách thành công"));
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

    @PostMapping("/categories")
    @Operation(summary = "Tạo danh mục mới")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Map<String, Object> body) {
        String name = body.getOrDefault("name", "").toString();
        Integer parentId = body.get("parentId") != null ? ((Number) body.get("parentId")).intValue() : null;
        String color = body.get("color") != null ? body.get("color").toString() : null;
        String iconClass = body.get("iconClass") != null ? body.get("iconClass").toString() : null;
        Category created = bookService.createCategory(name, parentId, color, iconClass);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Tạo danh mục thành công"));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Cập nhật tên danh mục")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable Integer id, @RequestBody Map<String, Object> body) {
        String name = body.get("name") != null ? body.get("name").toString() : null;
        Category updated = bookService.updateCategory(id, name);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật danh mục thành công"));
    }

    @PutMapping("/categories/{id}/full")
    @Operation(summary = "Cập nhật toàn bộ thông tin danh mục")
    public ResponseEntity<ApiResponse<Category>> updateCategoryFull(
            @PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Category updated = bookService.updateCategoryFull(id, body);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật danh mục thành công"));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Xóa danh mục")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> deleteCategory(@PathVariable Integer id) {
        bookService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", true), "Xóa danh mục thành công"));
    }

    /**
     * 📸 AI OCR: Phân tích ảnh bìa sách / mục lục và trích xuất Tên, Tác giả, ISBN, Mô tả.
     * Dùng Gemini Vision (gemini-1.5-flash). Kết quả điền sẵn vào form tạo sách.
     */
    @PostMapping("/extract-info")
    @Operation(
        summary = "AI trích xuất thông tin sách từ ảnh (OCR)",
        description = "Upload ảnh bìa sách hoặc trang mục lục, AI tự động nhận diện Tên sách, Tác giả, ISBN."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> extractBookInfo(
            @RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng chọn file ảnh", 400));
        }
        Map<String, Object> extracted = ragService.extractBookInfoFromImage(image);
        if (extracted.containsKey("error")) {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error(extracted.get("error").toString(), 503));
        }
        return ResponseEntity.ok(ApiResponse.success(extracted, "Trích xuất thông tin sách thành công"));
    }

    /**
     * 🔄 Bulk reindex tất cả sách vào Pinecone.
     * Gọi một lần sau khi cấu hình PINECONE_API_KEY và PINECONE_INDEX_URL.
     * Các sách tạo/cập nhật sau đó sẽ tự động được index.
     */
    @PostMapping("/reindex")
    @Operation(
        summary = "Bulk reindex tất cả sách vào Pinecone",
        description = "Tạo vector embedding cho toàn bộ sách trong DB và upsert vào Pinecone. Chạy một lần để seed index ban đầu."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> reindexAllBooks() {
        Map<String, Object> result = bookService.reindexAllBooks();
        return ResponseEntity.ok(ApiResponse.success(result, "Reindex hoàn tất"));
    }

    @GetMapping("/categories/{id}/book-count")
    @Operation(summary = "Lấy số lượng sách trong danh mục")
    public ResponseEntity<Integer> getCategoryBookCount(@PathVariable Integer id) {
        return ResponseEntity.ok(bookService.getCategoryBookCount(id));
    }

    @PostMapping("/categories/{fromId}/migrate")
    @Operation(summary = "Chuyển sách từ danh mục này sang danh mục khác")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> migrateBooks(
            @PathVariable Integer fromId, @RequestBody Map<String, Object> body) {
        Integer toId = ((Number) body.get("toCategoryId")).intValue();
        bookService.migrateBooksToCategory(fromId, toId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("migrated", true), "Chuyển sách thành công"));
    }

    @GetMapping("/{id}/ai-tags")
    @Operation(
        summary = "Lấy AI tags của sách",
        description = "Trả về danh sách tag được sinh tự động bởi AI (Gemini) dựa trên tên sách, thể loại và mô tả."
    )
    public ResponseEntity<ApiResponse<List<String>>> getAiTags(@PathVariable Integer id) {
        List<String> tags = bookService.getAiTags(id);
        return ResponseEntity.ok(ApiResponse.success(tags, "Lấy AI tags thành công"));
    }

    @PostMapping("/{id}/generate-description")
    @Operation(
        summary = "AI tự động tạo mô tả sách",
        description = "Dùng Gemini để tạo mô tả hấp dẫn 150-250 từ bằng tiếng Việt dựa trên tên sách, tác giả và thể loại."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> generateDescription(@PathVariable Integer id) {
        Books book = bookService.getBookById(id);
        List<String> authors = book.getAuthors() != null
            ? book.getAuthors().stream().map(Author::getName).toList() : List.of();
        List<String> categories = book.getCategories() != null
            ? book.getCategories().stream().map(Category::getName).toList() : List.of();
        String desc = aiTaggingService.generateDescription(book.getName(), authors, categories);
        if (desc == null)
            return ResponseEntity.status(503).body(ApiResponse.error("AI chưa sẵn sàng, vui lòng thử lại sau.", 503));
        return ResponseEntity.ok(ApiResponse.success(Map.of("description", desc), "Tạo mô tả thành công"));
    }

    @PostMapping("/describe")
    @Operation(
        summary = "AI preview mô tả sách (không cần ID)",
        description = "Dùng khi tạo sách mới: gửi tên, tác giả, thể loại để AI tạo mô tả trước khi lưu."
    )
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Map<String, String>>> previewDescription(@RequestBody Map<String, Object> body) {
        String name = (String) body.getOrDefault("name", "");
        List<String> authors = body.get("authors") instanceof List<?> listA
            ? (List<String>) listA : List.of();
        List<String> categories = body.get("categories") instanceof List<?> listC
            ? (List<String>) listC : List.of();
        String desc = aiTaggingService.generateDescription(name, authors, categories);
        if (desc == null)
            return ResponseEntity.status(503).body(ApiResponse.error("AI chưa sẵn sàng, vui lòng thử lại sau.", 503));
        return ResponseEntity.ok(ApiResponse.success(Map.of("description", desc), "Tạo mô tả thành công"));
    }
}
