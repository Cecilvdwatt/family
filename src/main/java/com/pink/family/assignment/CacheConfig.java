package com.pink.family.assignment;

import com.pink.family.assignment.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;

/**
 * Caching config. Set up cache managers using configurable values.
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    private final CacheProperties cacheProperties;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        CacheProperties.CacheSpec spec =
            cacheProperties.getCaches() == null?
                null:
                cacheProperties.getCaches().get(Constant.PERSON_BY_EXTERNAL_ID);

        cacheManager.registerCustomCache(Constant.PERSON_BY_EXTERNAL_ID, Caffeine.newBuilder()
            .expireAfterWrite(
                spec == null?
                    Duration.ofMinutes(Constant.DEFAULT_MINUTE_DURATION) :
                    spec.getExpireAfterWrite())
            .maximumSize(
                spec == null?
                    Constant.DEFAULT_MAX_SIZE :
                    spec.getMaximumSize())
            .recordStats()
            .build());

        spec =
            cacheProperties.getCaches() == null?
                null:
                cacheProperties.getCaches().get(Constant.PERSON_BY_EXTERNAL_ID);

        cacheManager.registerCustomCache(Constant.PERSONS_BY_NAME_DOB, Caffeine.newBuilder()
            .expireAfterWrite(
                spec == null?
                    Duration.ofMinutes(Constant.DEFAULT_MINUTE_DURATION) :
                    spec.getExpireAfterWrite())
            .maximumSize(
                spec == null?
                    Constant.DEFAULT_MAX_SIZE :
                    spec.getMaximumSize())
            .recordStats()
            .build());
        return cacheManager;
    }


    public static class Constant {
        public static final String PERSON_BY_EXTERNAL_ID = "personsByExternalIdCache";
        public static final String PERSONS_BY_NAME_DOB = "personsByNameSurnameCache";

        // Design note: just made up some default values here.
        public static int DEFAULT_MINUTE_DURATION = 10;
        public static int DEFAULT_MAX_SIZE = 1000;
    }

}