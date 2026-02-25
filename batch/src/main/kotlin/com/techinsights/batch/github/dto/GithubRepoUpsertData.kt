package com.techinsights.batch.github.dto

import java.time.LocalDateTime

data class GithubRepoUpsertData(
    val id: Long,
    val repoName: String,
    val fullName: String,
    val description: String?,
    val htmlUrl: String,
    val starCount: Long,
    val forkCount: Long,
    val primaryLanguage: String?,
    val ownerName: String,
    val ownerAvatarUrl: String?,
    /** DB 컬럼 topics TEXT: 쉼표 구분 문자열 */
    val topics: String?,
    val pushedAt: LocalDateTime,
    val fetchedAt: LocalDateTime,
)
