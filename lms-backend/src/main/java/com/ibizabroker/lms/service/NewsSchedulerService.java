package com.ibizabroker.lms.service;

import com.ibizabroker.lms.dao.NewsRepository;
import com.ibizabroker.lms.entity.News;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service to automatically publish scheduled draft news
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSchedulerService {

    private final NewsRepository newsRepository;

    /**
     * Check every 5 minutes for draft news that should be published
     */
    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000 ms
    @Transactional
    public void publishScheduledNews() {
        Instant now = Instant.now();
        List<News> readyToPublish = newsRepository.findDraftNewsReadyToPublish(now);
        
        if (!readyToPublish.isEmpty()) {
            log.info("Found {} draft news items ready to publish", readyToPublish.size());
            
            readyToPublish.forEach(news -> {
                news.setStatus(News.NewsStatus.PUBLISHED);
                newsRepository.save(news);
                log.info("Auto-published news: {} (ID: {})", news.getTitle(), news.getId());
            });
        }
    }
}
