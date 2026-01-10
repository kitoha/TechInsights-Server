package com.techinsights.domain.config.dataSource

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("spring.datasource.hikari")
class HikariProperties {
    var maximumPoolSize: Int = 10
    var minimumIdle: Int = 10
    var connectionTimeout: Long = 30000
    var idleTimeout: Long = 600000
    var maxLifetime: Long = 1800000
    var poolName: String = "HikariPool"
}
