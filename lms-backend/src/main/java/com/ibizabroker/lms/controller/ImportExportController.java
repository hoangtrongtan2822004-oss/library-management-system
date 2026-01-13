package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dto.ImportSummaryDto;
import com.ibizabroker.lms.service.ImportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class ImportExportController {

    private final ImportExportService importExportService;

    @PostMapping(value = "/import/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummaryDto importUsers(@RequestParam("file") MultipartFile file) {
        return importExportService.importUsers(file);
    }

    @PostMapping(value = "/import/books", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportSummaryDto importBooks(@RequestParam("file") MultipartFile file) {
        return importExportService.importBooks(file);
    }

    @SuppressWarnings("null")
    @GetMapping("/export/users")
    public ResponseEntity<ByteArrayResource> exportUsers() {
        ByteArrayResource data = importExportService.exportUsersWorkbook();
        @SuppressWarnings("null")
        MediaType contentType = importExportService.excelContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("users_export.xlsx").build().toString())
                .contentType(contentType)
                .contentLength(data.contentLength())
                .body(data);
    }

    @SuppressWarnings("null")
    @GetMapping("/export/books")
    public ResponseEntity<ByteArrayResource> exportBooks() {
        ByteArrayResource data = importExportService.exportBooksWorkbook();
        @SuppressWarnings("null")
        MediaType contentType = importExportService.excelContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("books_export.xlsx").build().toString())
                .contentType(contentType)
                .contentLength(data.contentLength())
                .body(data);
    }

    @SuppressWarnings("null")
    @GetMapping("/import/template/users")
    public ResponseEntity<ByteArrayResource> usersTemplate() {
        ByteArrayResource data = importExportService.usersTemplate();
        @SuppressWarnings("null")
        MediaType contentType = importExportService.excelContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("users_template.xlsx").build().toString())
                .contentType(contentType)
                .contentLength(data.contentLength())
                .body(data);
    }

    @SuppressWarnings("null")
    @GetMapping("/import/template/books")
    public ResponseEntity<ByteArrayResource> booksTemplate() {
        ByteArrayResource data = importExportService.booksTemplate();
        @SuppressWarnings("null")
        MediaType contentType = importExportService.excelContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("books_template.xlsx").build().toString())
                .contentType(contentType)
                .contentLength(data.contentLength())
                .body(data);
    }
}
