package com.techinsights.api.recommend

import com.techinsights.domain.dto.auth.Requester
import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.enums.Category
import com.techinsights.domain.service.recommend.RecommendationService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class RecommendControllerTest : FunSpec({
    val recommendationService = mockk<RecommendationService>()
    val controller = RecommendController(recommendationService)

    val sampleCompanyDto = CompanyDto(
        id = "company-1",
        name = "Tech Company",
        blogUrl = "https://techcompany.com/blog",
        logoImageName = "tech-logo.png",
        rssSupported = true
    )

    val samplePost1 = PostDto(
        id = "post-1",
        title = "Kotlin Best Practices",
        preview = "Learn the best practices",
        url = "https://techcompany.com/kotlin",
        content = "Kotlin content",
        publishedAt = LocalDateTime.now(),
        thumbnail = "kotlin.png",
        company = sampleCompanyDto,
        viewCount = 100,
        categories = setOf(Category.BackEnd),
        isSummary = true,
        isEmbedding = true
    )

    val samplePost2 = PostDto(
        id = "post-2",
        title = "Spring Boot Guide",
        preview = "Complete guide to Spring Boot",
        url = "https://techcompany.com/spring",
        content = "Spring content",
        publishedAt = LocalDateTime.now(),
        thumbnail = "spring.png",
        company = sampleCompanyDto,
        viewCount = 200,
        categories = setOf(Category.BackEnd),
        isSummary = true,
        isEmbedding = true
    )

    beforeTest {
        clearMocks(recommendationService)
    }

    test("익명 사용자의 추천 게시글 조회") {
        // given
        val requester = Requester.Anonymous("anonymous-123", "127.0.0.1")
        val recommendedPosts = listOf(samplePost1, samplePost2)

        every { recommendationService.getRecommendationsForUser(requester) } returns recommendedPosts

        // when
        val result = controller.getRecommendations(requester)

        // then
        result shouldHaveSize 2
        result[0].postId shouldBe "post-1"
        result[0].title shouldBe "Kotlin Best Practices"
        result[0].logoImageName shouldBe "tech-logo.png"
        result[1].postId shouldBe "post-2"
        result[1].title shouldBe "Spring Boot Guide"

        verify(exactly = 1) { recommendationService.getRecommendationsForUser(requester) }
    }

    test("인증된 사용자의 추천 게시글 조회") {
        // given
        val requester = Requester.Authenticated(1L, "127.0.0.1")
        val recommendedPosts = listOf(samplePost1)

        every { recommendationService.getRecommendationsForUser(requester) } returns recommendedPosts

        // when
        val result = controller.getRecommendations(requester)

        // then
        result shouldHaveSize 1
        result[0].postId shouldBe "post-1"
        result[0].title shouldBe "Kotlin Best Practices"

        verify(exactly = 1) { recommendationService.getRecommendationsForUser(requester) }
    }

    test("추천 게시글이 없는 경우 빈 리스트 반환") {
        // given
        val requester = Requester.Anonymous("anonymous-456", "127.0.0.1")

        every { recommendationService.getRecommendationsForUser(requester) } returns emptyList()

        // when
        val result = controller.getRecommendations(requester)

        // then
        result shouldHaveSize 0
        verify(exactly = 1) { recommendationService.getRecommendationsForUser(requester) }
    }

    test("PostRecommendResponse 변환이 올바르게 동작") {
        // given
        val requester = Requester.Authenticated(2L, "192.168.1.1")
        val recommendedPosts = listOf(samplePost1, samplePost2)

        every { recommendationService.getRecommendationsForUser(requester) } returns recommendedPosts

        // when
        val result = controller.getRecommendations(requester)

        // then
        result.forEach { response ->
            response.postId shouldBe recommendedPosts.find { it.id == response.postId }?.id
            response.title shouldBe recommendedPosts.find { it.id == response.postId }?.title
            response.logoImageName shouldBe "tech-logo.png"
        }
    }

    test("다양한 IP 주소로 요청 가능") {
        // given
        val ipAddresses = listOf("127.0.0.1", "192.168.0.1", "10.0.0.1", "::1")

        ipAddresses.forEach { ip ->
            val requester = Requester.Anonymous("anonymous-$ip", ip)
            every { recommendationService.getRecommendationsForUser(requester) } returns listOf(samplePost1)

            // when
            val result = controller.getRecommendations(requester)

            // then
            result shouldHaveSize 1
        }

        verify(exactly = ipAddresses.size) { recommendationService.getRecommendationsForUser(any()) }
    }
})
