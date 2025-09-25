package com.YouTubeTools.Config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.CacheManager;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setAllowNullValues(false);
        // Cache names for different operations
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "videoDetails",
                "videoSearch",
                "thumbnails"
        ));
        return cacheManager;
    }
}
