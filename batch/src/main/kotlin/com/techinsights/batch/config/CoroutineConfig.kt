package com.techinsights.batch.config

import kotlinx.coroutines.Dispatchers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig {

  @Bean("ioDispatcher")
  fun ioDispatcher() = Dispatchers.IO
}