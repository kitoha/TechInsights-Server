package com.techinsights.batch.github.readme.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * GitHub REST API GET /repos/{owner}/{repo}/readme 응답 DTO.
 * 실제 응답에는 더 많은 필드가 있지만, README 내용 추출에 필요한 필드만 매핑한다.
 */
data class GithubReadmeResponse(
    @JsonProperty("content") val content: String,
    @JsonProperty("encoding") val encoding: String = "base64",
)
