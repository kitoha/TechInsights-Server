package com.techinsights.domain.dto.github

import com.techinsights.domain.enums.GithubSortType
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

data class GithubRepositoryCursor(
    val sortType: GithubSortType,
    val primaryValue: String,
    val secondaryValue: String? = null,
    val id: Long,
) {
    fun encode(): String {
        val raw = listOfNotNull(sortType.name, primaryValue, secondaryValue, id.toString()).joinToString(":")
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
    }

    fun primaryAsLong(): Long = primaryValue.toLong()

    fun secondaryAsLong(): Long = secondaryValue?.toLong()
        ?: throw IllegalStateException("secondaryValue is required for $sortType cursor")

    fun primaryAsDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(primaryValue.toLong()), ZoneOffset.UTC)

    companion object {
        fun decode(encoded: String, expectedSortType: GithubSortType): GithubRepositoryCursor {
            val decoded = String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
            val parts = decoded.split(":")
            require(parts.isNotEmpty()) { "Invalid cursor format" }

            val sortType = GithubSortType.valueOf(parts[0])
            require(sortType == expectedSortType) { "Cursor sort type mismatch" }

            return when (sortType) {
                GithubSortType.STARS, GithubSortType.LATEST -> {
                    require(parts.size == 3) { "Invalid cursor format" }
                    GithubRepositoryCursor(
                        sortType = sortType,
                        primaryValue = parts[1],
                        id = parts[2].toLong(),
                    )
                }

                GithubSortType.TRENDING, GithubSortType.DAILY_TRENDING -> {
                    require(parts.size == 4) { "Invalid cursor format" }
                    GithubRepositoryCursor(
                        sortType = sortType,
                        primaryValue = parts[1],
                        secondaryValue = parts[2],
                        id = parts[3].toLong(),
                    )
                }
            }
        }

        fun fromDto(dto: GithubRepositoryDto, sortType: GithubSortType): GithubRepositoryCursor =
            when (sortType) {
                GithubSortType.STARS -> GithubRepositoryCursor(
                    sortType = sortType,
                    primaryValue = dto.starCount.toString(),
                    id = dto.id,
                )

                GithubSortType.LATEST -> GithubRepositoryCursor(
                    sortType = sortType,
                    primaryValue = dto.pushedAt.toInstant(ZoneOffset.UTC).toEpochMilli().toString(),
                    id = dto.id,
                )

                GithubSortType.TRENDING -> GithubRepositoryCursor(
                    sortType = sortType,
                    primaryValue = dto.weeklyStarDelta.toString(),
                    secondaryValue = dto.starCount.toString(),
                    id = dto.id,
                )

                GithubSortType.DAILY_TRENDING -> GithubRepositoryCursor(
                    sortType = sortType,
                    primaryValue = dto.dailyStarDelta.toString(),
                    secondaryValue = dto.starCount.toString(),
                    id = dto.id,
                )
            }
    }
}
