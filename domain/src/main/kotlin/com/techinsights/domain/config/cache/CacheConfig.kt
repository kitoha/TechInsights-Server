package com.techinsights.domain.config.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@EnableCaching
@Configuration
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val manager = CaffeineCacheManager()
        manager.registerCustomCache(
            GITHUB_TRENDING,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(50)
                .softValues()
                .build(),
        )
        manager.registerCustomCache(
            GITHUB_SEARCH,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(100)
                .softValues()
                .build(),
        )
        return manager
    }

    companion object {
        const val GITHUB_TRENDING = "githubTrending"
        const val GITHUB_SEARCH = "githubSearch"
    }
}
