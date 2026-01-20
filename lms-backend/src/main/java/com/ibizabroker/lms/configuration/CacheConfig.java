package com.ibizabroker.lms.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.default-ttl:5}")
    private int defaultTtl;

    @Value("${app.cache.book-details-ttl:10}")
    private int bookDetailsTtl;

    @Value("${app.cache.featured-books-ttl:10}")
    private int featuredBooksTtl;

    @Value("${app.cache.suggestions-ttl:3}")
    private int suggestionsTtl;

    @Value("${app.cache.system-settings-ttl:30}")
    private int systemSettingsTtl;

    @Bean
    @SuppressWarnings("null")
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(defaultTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "book-details", defaultConfig.entryTtl(Duration.ofMinutes(bookDetailsTtl)),
                "books-newest", defaultConfig.entryTtl(Duration.ofMinutes(defaultTtl)),
                "featured-books", defaultConfig.entryTtl(Duration.ofMinutes(featuredBooksTtl)),
                "similar-books", defaultConfig.entryTtl(Duration.ofMinutes(bookDetailsTtl)),
                "search-suggestions", defaultConfig.entryTtl(Duration.ofMinutes(suggestionsTtl)),
                "author-suggestions", defaultConfig.entryTtl(Duration.ofMinutes(suggestionsTtl)),
                "system-settings", defaultConfig.entryTtl(Duration.ofMinutes(systemSettingsTtl))
        );

        log.info("Redis Cache initialized: default={}m, book-details={}m, featured={}m, suggestions={}m, settings={}m",
                defaultTtl, bookDetailsTtl, featuredBooksTtl, suggestionsTtl, systemSettingsTtl);

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
