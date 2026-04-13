package com.techinsights.batch.github.community.service

import com.techinsights.batch.github.community.config.props.CommunityApiProperties
import com.techinsights.batch.github.community.dto.HnSearchResponse
import com.techinsights.batch.github.community.dto.RedditSearchResponse
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.util.function.Function

class CommunityFetcherImplTest : FunSpec({

    val hnWebClient = mockk<WebClient>()
    val redditWebClient = mockk<WebClient>()
    val rateLimiterRegistry = mockk<RateLimiterRegistry>()

    val hnUriSpec = mockk<WebClient.RequestHeadersUriSpec<*>>()
    val hnHeadersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
    val hnResponseSpec = mockk<WebClient.ResponseSpec>()

    val redditUriSpec = mockk<WebClient.RequestHeadersUriSpec<*>>()
    val redditHeadersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
    val redditResponseSpec = mockk<WebClient.ResponseSpec>()

    val unlimitedConfig = RateLimiterConfig.custom()
        .limitForPeriod(Int.MAX_VALUE)
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .timeoutDuration(Duration.ofSeconds(5))
        .build()
    val hnRateLimiter = RateLimiter.of("hnApi-test", unlimitedConfig)
    val redditRateLimiter = RateLimiter.of("redditApi-test", unlimitedConfig)

    beforeTest {
        clearAllMocks()
        every { rateLimiterRegistry.rateLimiter("hnApi") } returns hnRateLimiter
        every { rateLimiterRegistry.rateLimiter("redditApi") } returns redditRateLimiter

        every { hnWebClient.get() } returns hnUriSpec
        every { hnUriSpec.uri(any<Function<UriBuilder, URI>>()) } returns hnHeadersSpec
        every { hnHeadersSpec.retrieve() } returns hnResponseSpec

        every { redditWebClient.get() } returns redditUriSpec
        every { redditUriSpec.uri(any<Function<UriBuilder, URI>>()) } returns redditHeadersSpec
        every { redditHeadersSpec.retrieve() } returns redditResponseSpec
    }

    val communityApiProperties = CommunityApiProperties()

    fun fetcher() = CommunityFetcherImpl(hnWebClient, redditWebClient, rateLimiterRegistry, communityApiProperties)

    fun mockHn(response: HnSearchResponse) {
        every { hnResponseSpec.bodyToMono(any<ParameterizedTypeReference<HnSearchResponse>>()) } returns Mono.just(response)
    }

    fun mockReddit(response: RedditSearchResponse) {
        every { redditResponseSpec.bodyToMono(any<ParameterizedTypeReference<RedditSearchResponse>>()) } returns Mono.just(response)
    }

    fun mockHnError(ex: Exception) {
        every { hnResponseSpec.bodyToMono(any<ParameterizedTypeReference<HnSearchResponse>>()) } returns Mono.error(ex)
    }

    fun mockRedditError(ex: Exception) {
        every { redditResponseSpec.bodyToMono(any<ParameterizedTypeReference<RedditSearchResponse>>()) } returns Mono.error(ex)
    }

    context("HN + Reddit 정상 응답") {

        test("HN과 Reddit 게시글이 모두 포함된 Input을 반환한다") {
            mockHn(buildHnResponse(listOf(buildHnHit("id1", "HN Post 1", points = 100))))
            mockReddit(buildRedditResponse(listOf(buildRedditPost("Reddit Post 1", score = 50))))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.hnPosts shouldHaveSize 1
            result.redditPosts shouldHaveSize 1
            result.totalMentions shouldBe 2
        }

        test("repoFullName, ownerName, repoName이 Input에 그대로 담긴다") {
            mockHn(buildHnResponse(emptyList()))
            mockReddit(buildRedditResponse(emptyList()))

            val result = fetcher().fetch("owner/repo", "owner", "repo", prevMentionCount = 3, updateCount = 1)

            result.repoFullName shouldBe "owner/repo"
            result.ownerName shouldBe "owner"
            result.repoName shouldBe "repo"
            result.prevMentionCount shouldBe 3
            result.updateCount shouldBe 1
        }
    }

    context("HN 게시글 변환") {

        test("title이 null인 HN hit은 필터링된다") {
            mockHn(buildHnResponse(listOf(buildHnHit("id1", title = null))))
            mockReddit(buildRedditResponse(emptyList()))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.hnPosts.shouldBeEmpty()
        }

        test("HN url이 있으면 그 값을 사용한다") {
            mockHn(buildHnResponse(listOf(buildHnHit("id1", "Post", url = "https://example.com/post"))))
            mockReddit(buildRedditResponse(emptyList()))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.hnPosts[0].url shouldBe "https://example.com/post"
        }

        test("HN url이 없으면 objectId 기반 HN URL을 사용한다") {
            mockHn(buildHnResponse(listOf(buildHnHit("12345", "Post", url = null))))
            mockReddit(buildRedditResponse(emptyList()))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.hnPosts[0].url shouldBe "${communityApiProperties.hn.itemBaseUrl}/item?id=12345"
        }

        test("HN CommunityPost 필드가 올바르게 매핑된다") {
            mockHn(buildHnResponse(listOf(buildHnHit("id1", "Post Title", points = 42, numComments = 7, author = "hnuser", url = "https://example.com"))))
            mockReddit(buildRedditResponse(emptyList()))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)
            val post = result.hnPosts[0]

            post.platform shouldBe "hn"
            post.title shouldBe "Post Title"
            post.score shouldBe 42
            post.commentCount shouldBe 7
            post.username shouldBe "hnuser"
        }
    }

    context("Reddit 게시글 변환") {

        test("title이 null인 Reddit post는 필터링된다") {
            mockHn(buildHnResponse(emptyList()))
            mockReddit(buildRedditResponse(listOf(buildRedditPost(title = null))))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.redditPosts.shouldBeEmpty()
        }

        test("Reddit permalink가 있으면 reddit.com 도메인을 붙여 URL을 생성한다") {
            mockHn(buildHnResponse(emptyList()))
            mockReddit(buildRedditResponse(listOf(buildRedditPost("Post", permalink = "/r/programming/comments/abc"))))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.redditPosts[0].url shouldBe "${communityApiProperties.reddit.baseUrl}/r/programming/comments/abc"
        }

        test("Reddit permalink가 없으면 url 필드를 사용한다") {
            mockHn(buildHnResponse(emptyList()))
            mockReddit(buildRedditResponse(listOf(buildRedditPost("Post", permalink = null, url = "https://example.com"))))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.redditPosts[0].url shouldBe "https://example.com"
        }

        test("Reddit permalink도 url도 없으면 필터링된다") {
            mockHn(buildHnResponse(emptyList()))
            mockReddit(buildRedditResponse(listOf(buildRedditPost("Post", permalink = null, url = null))))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.redditPosts.shouldBeEmpty()
        }

        test("Reddit CommunityPost 필드가 올바르게 매핑된다") {
            mockHn(buildHnResponse(emptyList()))
            mockReddit(buildRedditResponse(listOf(buildRedditPost("Post Title", score = 99, numComments = 15, author = "redditor", permalink = "/r/test/abc"))))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)
            val post = result.redditPosts[0]

            post.platform shouldBe "reddit"
            post.title shouldBe "Post Title"
            post.score shouldBe 99
            post.commentCount shouldBe 15
            post.username shouldBe "redditor"
        }
    }

    context("상위 5개 제한") {

        test("HN hits가 5개를 넘으면 상위 5개만 반환한다") {
            val hits = (1..8).map { buildHnHit("id$it", "Post $it", points = it * 10) }
            mockHn(buildHnResponse(hits))
            mockReddit(buildRedditResponse(emptyList()))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.hnPosts shouldHaveSize 5
            result.hnPosts[0].score shouldBe 80
        }

        test("Reddit posts가 5개를 넘으면 상위 5개만 반환한다") {
            val posts = (1..8).map { buildRedditPost("Post $it", score = it * 10) }
            mockHn(buildHnResponse(emptyList()))
            mockReddit(buildRedditResponse(posts))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.redditPosts shouldHaveSize 5
            result.redditPosts[0].score shouldBe 80
        }
    }

    context("API 실패 처리") {

        test("HN API 실패 시 hnPosts를 빈 리스트로 반환한다") {
            mockHnError(WebClientResponseException.create(429, "Too Many Requests", org.springframework.http.HttpHeaders.EMPTY, ByteArray(0), null))
            mockReddit(buildRedditResponse(listOf(buildRedditPost("Reddit Post"))))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.hnPosts.shouldBeEmpty()
            result.redditPosts shouldHaveSize 1
        }

        test("Reddit API 실패 시 redditPosts를 빈 리스트로 반환한다") {
            mockHn(buildHnResponse(listOf(buildHnHit("id1", "HN Post"))))
            mockRedditError(WebClientResponseException.create(503, "Service Unavailable", org.springframework.http.HttpHeaders.EMPTY, ByteArray(0), null))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.hnPosts shouldHaveSize 1
            result.redditPosts.shouldBeEmpty()
        }

        test("HN과 Reddit 모두 실패 시 totalMentions는 0이다") {
            mockHnError(RuntimeException("HN error"))
            mockRedditError(RuntimeException("Reddit error"))

            val result = fetcher().fetch("owner/repo", "owner", "repo", null, 0)

            result.totalMentions shouldBe 0
        }
    }
})

private fun buildHnResponse(hits: List<HnSearchResponse.HnHit>) = HnSearchResponse(hits = hits)

private fun buildHnHit(
    objectId: String = "id1",
    title: String? = "Test Post",
    points: Int = 10,
    numComments: Int = 5,
    author: String? = "user",
    url: String? = "https://example.com",
) = HnSearchResponse.HnHit(
    objectId = objectId,
    title = title,
    url = url,
    points = points,
    numComments = numComments,
    author = author,
)

private fun buildRedditResponse(posts: List<RedditSearchResponse.RedditPost>) = RedditSearchResponse(
    data = RedditSearchResponse.RedditData(
        children = posts.map { RedditSearchResponse.RedditChild(data = it) }
    )
)

private fun buildRedditPost(
    title: String? = "Test Post",
    score: Int = 10,
    numComments: Int = 3,
    author: String? = "redditor",
    permalink: String? = "/r/programming/comments/test",
    url: String? = "https://example.com",
) = RedditSearchResponse.RedditPost(
    title = title,
    url = url,
    score = score,
    numComments = numComments,
    subreddit = "programming",
    permalink = permalink,
    author = author,
)
