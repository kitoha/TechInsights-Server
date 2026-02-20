package com.techinsights.domain.config.datasource

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("db")
class DataSourceProperties {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
    var driverClassName: String = "org.postgresql.Driver"
}