package com.techinsights.batch.github.readme.processor

import com.fasterxml.jackson.databind.ObjectMapper
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.github.GithubRepositoryDto
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.Base64

@Component
class GithubReadmeFetchProcessor(
    @Qualifier("githubWebClient") private val webClient: WebClient,
) : ItemProcessor<GithubRepositoryDto, ArticleInput> {

    private val mapper = ObjectMapper()

    override fun process(item: GithubRepositoryDto): ArticleInput? {
        return try {
            val jsonBody = webClient.get()
                .uri("/repos/${item.fullName}/readme")
                .retrieve()
                .onStatus({ status -> status.is4xxClientError }) { response ->
                    response.createException()
                }
                .bodyToMono(String::class.java)
                .block()
                ?: return null

            val tree = mapper.readTree(jsonBody)
            val encodedContent = tree.path("content").asText("")

            if (encodedContent.isEmpty()) {
                log.debug("[ReadmeFetch] Empty README content for ${item.fullName}, skipping")
                return null
            }

            val decodedContent = decodeBase64(encodedContent)

            if (decodedContent.isBlank()) {
                log.debug("[ReadmeFetch] Empty README for ${item.fullName}, skipping")
                return null
            }

            ArticleInput(
                id = item.fullName,
                title = item.repoName,
                content = decodedContent.take(README_MAX_CHARS),
            )
        } catch (e: WebClientResponseException.NotFound) {
            log.debug("[ReadmeFetch] No README for ${item.fullName}")
            null
        } catch (e: Exception) {
            log.warn("[ReadmeFetch] Failed to fetch README for ${item.fullName}: ${e.message}")
            null
        }
    }

    private fun decodeBase64(encoded: String): String {
        val cleanEncoded = encoded.replace("\n", "")
        val decoded = Base64.getMimeDecoder().decode(cleanEncoded)
        return String(decoded, Charsets.UTF_8)
    }

    companion object {
        private val log = LoggerFactory.getLogger(GithubReadmeFetchProcessor::class.java)
        private const val README_MAX_CHARS = 2000
    }
}
