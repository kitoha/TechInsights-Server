package com.techinsights.batch.github.community.service

import com.techinsights.batch.github.community.config.props.CommunityApiProperties
import com.techinsights.batch.github.community.dto.CommunityBuzzInput
import com.techinsights.batch.github.community.dto.HnSearchResponse
import com.techinsights.batch.github.community.dto.RedditSearchResponse
import com.techinsights.domain.dto.community.CommunityPost
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class CommunityFetcherImpl(
    @Qualifier("hnWebClient") private val hnWebClient: WebClient,
    @Qualifier("redditWebClient") private val redditWebClient: WebClient,
    private val rateLimiterRegistry: RateLimiterRegistry,
    private val communityApiProperties: CommunityApiProperties,
) : CommunityFetcher {

    private val hnRateLimiter by lazy { rateLimiterRegistry.rateLimiter("hnApi") }
    private val redditRateLimiter by lazy { rateLimiterRegistry.rateLimiter("redditApi") }

    override suspend fun fetch(
        repoFullName: String,
        ownerName: String,
        repoName: String,
        prevMentionCount: Int?,
        updateCount: Int,
    ): CommunityBuzzInput = coroutineScope {
        val hnDeferred = async { fetchHn(ownerName, repoName) }
        val redditDeferred = async { fetchReddit(ownerName, repoName) }

        CommunityBuzzInput(
            repoFullName = repoFullName,
            ownerName = ownerName,
            repoName = repoName,
            hnPosts = hnDeferred.await(),
            redditPosts = redditDeferred.await(),
            prevMentionCount = prevMentionCount,
            updateCount = updateCount,
        )
    }

    private suspend fun fetchHn(ownerName: String, repoName: String): List<CommunityPost> =
        hnRateLimiter.executeSuspendFunction {
            runCatching {
                hnWebClient.get()
                    .uri { it.path("/api/v1/search")
                        .queryParam("query", "$ownerName/$repoName")
                        .queryParam("tags", "story")
                        .queryParam("hitsPerPage", FETCH_LIMIT)
                        .build()
                    }
                    .retrieve()
                    .awaitBody<HnSearchResponse>()
                    .top5()
                    .mapNotNull { convertHnPost(it) }
            }.getOrElse { e ->
                if (e is CancellationException) throw e
                log.warn("[HN] fetch failed for $ownerName/$repoName: ${e.message}")
                emptyList()
            }
        }

    private suspend fun fetchReddit(ownerName: String, repoName: String): List<CommunityPost> =
        redditRateLimiter.executeSuspendFunction {
            runCatching {
                redditWebClient.get()
                    .uri { it.path("/search.json")
                        .queryParam("q", "$ownerName $repoName")
                        .queryParam("sort", "relevance")
                        .queryParam("limit", FETCH_LIMIT)
                        .queryParam("type", "link")
                        .build()
                    }
                    .retrieve()
                    .awaitBody<RedditSearchResponse>()
                    .top5()
                    .mapNotNull { convertRedditPost(it) }
            }.getOrElse { e ->
                if (e is CancellationException) throw e
                log.warn("[Reddit] fetch failed for $ownerName/$repoName: ${e.message}")
                emptyList()
            }
        }

    private fun convertHnPost(hit: HnSearchResponse.HnHit): CommunityPost? {
        val title = hit.title ?: return null
        return CommunityPost(
            platform = PLATFORM_HN,
            title = title,
            score = hit.points,
            commentCount = hit.numComments,
            username = hit.author,
            url = hit.url ?: "${communityApiProperties.hn.itemBaseUrl}/item?id=${hit.objectId}",
        )
    }

    private fun convertRedditPost(post: RedditSearchResponse.RedditPost): CommunityPost? {
        val title = post.title ?: return null
        val url = post.permalink?.let { "${communityApiProperties.reddit.baseUrl}$it" }
            ?: post.url
            ?: return null
        return CommunityPost(
            platform = PLATFORM_REDDIT,
            title = title,
            score = post.score,
            commentCount = post.numComments,
            username = post.author,
            url = url,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommunityFetcherImpl::class.java)
        private const val FETCH_LIMIT = 10
        const val PLATFORM_HN = "hn"
        const val PLATFORM_REDDIT = "reddit"
    }
}
