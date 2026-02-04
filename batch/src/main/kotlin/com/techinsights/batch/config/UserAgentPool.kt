package com.techinsights.batch.config

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class UserAgentPool {
    private val agents = listOf(
        // Chrome (Windows)
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        // Chrome (Mac)
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        // Firefox
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        // Safari
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    )

    private val domainAgentCache = ConcurrentHashMap<String, String>()

    fun getUserAgent(domain: String): String {
        return domainAgentCache.computeIfAbsent(domain) {
            agents.random()
        }
    }
}
