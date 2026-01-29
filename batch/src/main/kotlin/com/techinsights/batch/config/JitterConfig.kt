package com.techinsights.batch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "crawler.jitter")
class JitterConfig {
    var enabled: Boolean = true
    var minMs: Long = 100
    var maxMs: Long = 2000
}
