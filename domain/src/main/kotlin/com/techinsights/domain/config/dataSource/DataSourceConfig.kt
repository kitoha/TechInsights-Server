package com.techinsights.domain.config.dataSource

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DataSourceConfig (
  private val dataSourceProperties: DataSourceProperties
){
  @Bean
  fun dataSource() : DataSource =
    DataSourceBuilder.create()
      .type(HikariDataSource::class.java)
      .driverClassName("com.mysql.cj.jdbc.Driver")
      .url(dataSourceProperties.url)
      .username(dataSourceProperties.username)
      .password(dataSourceProperties.password)
      .build()
}