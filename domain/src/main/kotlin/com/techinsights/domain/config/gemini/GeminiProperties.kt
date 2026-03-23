package com.techinsights.domain.config.gemini

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("gemini")
data class GeminiProperties(
    var apiKey: String = "",
    /** 단건 아티클 요약 시 최대 출력 토큰. 고정값 사용 (1요청 = 1아티클). */
    var maxOutputTokens: Int = 65_536,
    /** 배치 요청당 총 토큰 예산. TPM 250K ÷ RPM 2 = 125K 이론값, 안전 마진으로 100K 설정. */
    var maxTokensPerRequest: Int = 100_000,
    /** 배치 항목당 입력 토큰 추정값. README 2000자 ≈ 500 토큰. */
    var inputTokensPerItem: Int = 500,
    /** 배치 항목당 출력 목표 토큰. 한국어 요약 2문장 + JSON 필드. */
    var outputTokensPerItem: Int = 200,
)