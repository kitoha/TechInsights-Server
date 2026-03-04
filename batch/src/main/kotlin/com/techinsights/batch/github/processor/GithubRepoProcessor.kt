package com.techinsights.batch.github.processor

import com.techinsights.batch.github.dto.GithubRepoUpsertData
import com.techinsights.batch.github.dto.GithubSearchResponse
import com.techinsights.domain.utils.Tsid
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Component
class GithubRepoProcessor : ItemProcessor<GithubSearchResponse.Item, GithubRepoUpsertData> {

    override fun process(item: GithubSearchResponse.Item): GithubRepoUpsertData {
        return GithubRepoUpsertData(
            id = Tsid.generateLong(),
            repoName = item.name,
            fullName = item.fullName,
            description = item.description,
            htmlUrl = item.htmlUrl,
            starCount = item.stargazersCount,
            forkCount = item.forksCount,
            primaryLanguage = item.language,
            ownerName = item.owner.login,
            ownerAvatarUrl = item.owner.avatarUrl,
            topics = item.topics.takeIf { it.isNotEmpty() }?.joinToString(","),
            pushedAt = parseDateTime(item.pushedAt),
            fetchedAt = LocalDateTime.now(),
        )
    }

    private fun parseDateTime(dateStr: String): LocalDateTime =
        OffsetDateTime.parse(dateStr).toLocalDateTime()
}
