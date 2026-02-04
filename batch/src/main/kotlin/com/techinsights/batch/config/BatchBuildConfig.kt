package com.techinsights.batch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "batch.build")
data class BatchBuildConfig(
    var maxTokensPerRequest: Int = 200_000,
    var maxBatchSize: Int = 7,
    var basePromptTokens: Int = 500,
    var avgTokensPerSummary: Int = 2500,
    var jsonOverheadTokens: Int = 500,
    var outputSafetyMargin: Double = 0.9,
    var truncationBufferTokens: Int = 2000,
    var tokensPerChar: Int = 3
)
