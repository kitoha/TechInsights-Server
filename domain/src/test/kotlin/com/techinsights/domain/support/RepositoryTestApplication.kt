package com.techinsights.domain.support

import com.techinsights.domain.config.JpaAuditingConfig
import com.techinsights.domain.config.QuerydslConfig
import com.techinsights.domain.config.search.SearchConfig
import com.techinsights.domain.config.search.SearchProperties
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.techinsights.domain.entity")
@EnableJpaRepositories("com.techinsights.domain.repository")
@ComponentScan("com.techinsights.domain.repository")
@Import(QuerydslConfig::class, JpaAuditingConfig::class, SearchConfig::class, SearchProperties::class)
class RepositoryTestApplication
