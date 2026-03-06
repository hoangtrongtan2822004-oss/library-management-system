package com.ibizabroker.lms.service;

import com.ibizabroker.lms.entity.Books;
import com.ibizabroker.lms.entity.Users;
import com.ibizabroker.lms.entity.Wishlist;
import com.ibizabroker.lms.dao.BooksRepository;
import com.ibizabroker.lms.dao.UsersRepository;
import com.ibizabroker.lms.dao.WishlistRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@SuppressWarnings("null")
public class WishlistService {

    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private UsersRepository usersRepository;
    @Autowired private BooksRepository booksRepository;

    // Hàm tiện ích lấy User đang đăng nhập
    private Users getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void addToWishlist(Integer bookId) {
        Users user = getCurrentUser();
        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        if (!wishlistRepository.existsByUserAndBook(user, book)) {
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            wishlist.setBook(book);
            wishlistRepository.save(wishlist);
        }
    }

    public void removeFromWishlist(Integer bookId) {
        Users user = getCurrentUser();
        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));
        wishlistRepository.deleteByUserAndBook(user, book);
    }

    public boolean isWishlisted(Integer bookId) {
        try {
            Users user = getCurrentUser();
            Books book = booksRepository.findById(bookId).orElse(null);
            if (book == null) return false;
            return wishlistRepository.existsByUserAndBook(user, book);
        } catch (Exception e) {
            return false; // Chưa đăng nhập
        }
    }
    
    public List<Wishlist> getMyWishlist() {
        Users user = getCurrentUser();
        return wishlistRepository.findByUser(user);
    }
}