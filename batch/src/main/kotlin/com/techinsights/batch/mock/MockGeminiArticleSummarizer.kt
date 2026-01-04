package com.techinsights.batch.mock

import com.techinsights.domain.dto.gemini.SummaryResult
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.gemini.ArticleSummarizer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import kotlin.random.Random


@Service
@Primary
@Profile("perf-test")
class MockGeminiArticleSummarizer : ArticleSummarizer {

    private val log = LoggerFactory.getLogger(MockGeminiArticleSummarizer::class.java)

    companion object {
        // 실제 Gemini API 평균 레이턴시 (로그 분석 결과: 1.5초)
        private const val API_RESPONSE_TIME_MS = 1500L

        private val MOCK_CATEGORIES = listOf("FrontEnd", "BackEnd", "AI", "BigData", "Infra", "Architecture")
    }

    override fun summarize(article: String, modelType: GeminiModelType): SummaryResult {
        val startTime = System.currentTimeMillis()

        Thread.sleep(API_RESPONSE_TIME_MS)

        val categories = MOCK_CATEGORIES[Random.nextInt(MOCK_CATEGORIES.size)]
        val preview = article.take(100).replace("\n", " ")

        val elapsedTime = System.currentTimeMillis() - startTime
        log.debug("Mock summarization completed in {}ms (article length: {})", elapsedTime, article.length)

        return SummaryResult(
            summary = "MOCK_SUMMARY: $preview... 이 글은 ${categories}에 대한 기술 블로그 포스트입니다.",
            preview = "MOCK_PREVIEW: $preview",
            categories = listOf(categories)
        )
    }
}
