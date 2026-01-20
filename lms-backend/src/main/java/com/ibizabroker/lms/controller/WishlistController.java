package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dao.WishlistRepository;
import com.ibizabroker.lms.dto.WishlistDto;
import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.entity.Wishlist;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
@Tag(name = "Wishlist")
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final UsersRepository usersRepository;
    private final BooksRepository booksRepository;

    public WishlistController(WishlistRepository wishlistRepository,
                              UsersRepository usersRepository,
                              BooksRepository booksRepository) {
        this.wishlistRepository = wishlistRepository;
        this.usersRepository = usersRepository;
        this.booksRepository = booksRepository;
    }

    @GetMapping(value = "/my-wishlist", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get current user's wishlist with full details")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getMyWishlist(@RequestParam(required = false, defaultValue = "recent") String sort) {
        Users user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status","error","error","Unauthorized"));
        
        List<Wishlist> entries;
        switch (sort.toLowerCase()) {
            case "name":
                entries = wishlistRepository.findByUserOrderByBook_NameAsc(user);
                break;
            case "recent":
            default:
                entries = wishlistRepository.findByUserOrderByCreatedAtDesc(user);
                break;
        }
        
        // Convert to DTOs with full book details and availability
        List<WishlistDto> dtos = entries.stream().map(this::convertToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping(value = "/add/{bookId}")
    @Operation(summary = "Add a book to wishlist")
    public ResponseEntity<?> add(@PathVariable Integer bookId) {
        Users user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status","error","error","Unauthorized"));
        Books book = booksRepository.findById(bookId).orElse(null);
        if (book == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status","error","error","Book not found"));
        if (!wishlistRepository.existsByUserAndBook(user, book)) {
            Wishlist w = new Wishlist();
            w.setUser(user);
            w.setBook(book);
            wishlistRepository.save(w);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status","ok"));
    }

    @DeleteMapping(value = "/remove/{bookId}")
    @Operation(summary = "Remove a book from wishlist")
    public ResponseEntity<?> remove(@PathVariable Integer bookId) {
        Users user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status","error","error","Unauthorized"));
        Books book = booksRepository.findById(bookId).orElse(null);
        if (book == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status","error","error","Book not found"));
        wishlistRepository.deleteByUserAndBook(user, book);
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PutMapping(value = "/update/{bookId}")
    @Operation(summary = "Update personal notes for a wishlist item")
    public ResponseEntity<?> updateNotes(@PathVariable Integer bookId, @RequestBody Map<String, String> payload) {
        Users user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status","error","error","Unauthorized"));
        Books book = booksRepository.findById(bookId).orElse(null);
        if (book == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status","error","error","Book not found"));
        
        Wishlist wishlist = wishlistRepository.findByUserAndBook(user, book).orElse(null);
        if (wishlist == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status","error","error","Wishlist entry not found"));
        
        wishlist.setNotes(payload.get("notes"));
        wishlist.setUpdatedAt(LocalDateTime.now());
        wishlistRepository.save(wishlist);
        
        return ResponseEntity.ok(Map.of("status","ok", "message", "Notes updated successfully"));
    }

    @GetMapping(value = "/check/{bookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Check if a book is in wishlist")
    public ResponseEntity<Boolean> check(@PathVariable Integer bookId) {
        Users user = currentUser();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Books book = booksRepository.findById(bookId).orElse(null);
        if (book == null) return ResponseEntity.ok(false);
        boolean exists = wishlistRepository.existsByUserAndBook(user, book);
        return ResponseEntity.ok(exists);
    }

    private Users currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return usersRepository.findByUsername(auth.getName()).orElse(null);
    }

    private WishlistDto convertToDto(Wishlist wishlist) {
        Books book = wishlist.getBook();
        WishlistDto dto = new WishlistDto();
        dto.setWishlistId(wishlist.getId());
        dto.setBookId(book.getId());
        dto.setBookName(book.getName());
        dto.setBookIsbn(book.getIsbn());
        dto.setCoverUrl(book.getCoverUrl());
        dto.setAvailableCopies(book.getNumberOfCopiesAvailable());
        dto.setNotes(wishlist.getNotes());
        dto.setAddedDate(wishlist.getCreatedAt());
        dto.setUpdatedDate(wishlist.getUpdatedAt());
        
        // Get first author if exists
        if (book.getAuthors() != null && !book.getAuthors().isEmpty()) {
            dto.setAuthorName(book.getAuthors().iterator().next().getName());
        }
        
        // Get first category if exists
        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            dto.setCategoryName(book.getCategories().iterator().next().getName());
        }
        
        return dto;
    }
}
