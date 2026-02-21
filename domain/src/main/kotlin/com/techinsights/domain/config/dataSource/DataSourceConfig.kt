package com.techinsights.domain.config.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DataSourceConfig (
  private val dataSourceProperties: DataSourceProperties,
  private val hikariProperties: HikariProperties
){
  @Bean
  fun dataSource() : DataSource {
    val config = HikariConfig()
    config.driverClassName = dataSourceProperties.driverClassName
    config.jdbcUrl = dataSourceProperties.url
    config.username = dataSourceProperties.username
    config.password = dataSourceProperties.password

    config.maximumPoolSize = hikariProperties.maximumPoolSize
    config.minimumIdle = hikariProperties.minimumIdle
    config.connectionTimeout = hikariProperties.connectionTimeout
    config.idleTimeout = hikariProperties.idleTimeout
    config.maxLifetime = hikariProperties.maxLifetime
    config.poolName = hikariProperties.poolName

    return HikariDataSource(config)
  }
}
