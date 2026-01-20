package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.CategoryRepository;
import com.ibizabroker.lms.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/categories")
@CrossOrigin("http://localhost:4200")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @SuppressWarnings("null")
    @GetMapping("/{id}")
    public Category getCategoryById(@PathVariable Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }
}
