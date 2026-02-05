package com.ibizabroker.lms.controller;

import com.ibizabroker.lms.dao.NewsRepository;
import com.ibizabroker.lms.entity.News;
import com.ibizabroker.lms.dto.NewsDto;
import java.util.stream.Collectors;
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
    public List<NewsDto> getLatestNews(@RequestParam(defaultValue = "5") int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return newsRepository.findAll(pageable)
                .getContent()
                .stream()
                .map(n -> NewsDto.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .coverImageUrl(n.getCoverImageUrl())
                        .publishedAt(n.getPublishedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("null")
        @GetMapping("/{id}")
        public NewsDto getNewsById(@PathVariable Long id) {
        News n = newsRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        return NewsDto.builder()
            .id(n.getId())
            .title(n.getTitle())
            .coverImageUrl(n.getCoverImageUrl())
            .publishedAt(n.getPublishedAt())
            .content(n.getContent())
            .build();
        }
}
