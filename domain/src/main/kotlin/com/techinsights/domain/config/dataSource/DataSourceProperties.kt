package com.techinsights.domain.config.dataSource

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("db")
class DataSourceProperties {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
    var driverClassName: String = "com.mysql.cj.jdbc.Driver"
}