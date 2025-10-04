package com.techinsights.domain.config.search

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SearchConfig {

  @Bean
  fun scoreWeights(searchProperties: SearchProperties): ScoreWeights {
    return searchProperties.score.weights
  }
}