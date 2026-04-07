package com.techinsights.domain.config.cache

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.cache.caffeine.CaffeineCacheManager

class CacheConfigTest : FunSpec({

    val config = CacheConfig()
    val cacheManager = config.cacheManager()

    test("cacheManager는 CaffeineCacheManager를 반환한다") {
        cacheManager.shouldBeInstanceOf<CaffeineCacheManager>()
    }

    test("githubTrending 캐시가 등록된다") {
        cacheManager.getCache(CacheConfig.GITHUB_TRENDING) shouldNotBe null
    }

    test("githubSearch 캐시가 등록된다") {
        cacheManager.getCache(CacheConfig.GITHUB_SEARCH) shouldNotBe null
    }
})
