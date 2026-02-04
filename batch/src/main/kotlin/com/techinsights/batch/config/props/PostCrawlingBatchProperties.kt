package com.techinsights.batch.config.props

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "batch.post-crawling")
class PostCrawlingBatchProperties {
  var jobName: String = "crawlPostJob"
  var stepName: String = "crawlPostStep"
  var chunkSize: Int = 10
  var retryLimit: Int = 3
  var corePoolSize: Int = 6
  var maxPoolSize: Int = 6
}