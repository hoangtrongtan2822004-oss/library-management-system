package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.NewsRepository;
import com.ibizabroker.lms.entity.News;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/news")
@CrossOrigin("http://localhost:4200")
@RequiredArgsConstructor
public class PublicNewsController {

    private final NewsRepository newsRepository;

    @GetMapping("/latest")
    public List<News> getLatestNews(@RequestParam(defaultValue = "5") int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return newsRepository.findAll(pageable).getContent();
    }

    @SuppressWarnings("null")
    @GetMapping("/{id}")
    public News getNewsById(@PathVariable Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
    }
}
