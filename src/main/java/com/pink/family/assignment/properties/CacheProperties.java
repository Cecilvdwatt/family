package com.pink.family.assignment.properties;


import com.pink.family.assignment.CacheConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "pink.config")
public class CacheProperties {

    private Map<String, CacheSpec> caches;


    @Data
    public static class CacheSpec {
        private Duration expireAfterWrite = Duration.ofMinutes(CacheConfig.Constant.DEFAULT_MINUTE_DURATION);
        private long maximumSize = CacheConfig.Constant.DEFAULT_MAX_SIZE;
    }
}
