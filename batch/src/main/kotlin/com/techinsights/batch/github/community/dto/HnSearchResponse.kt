package com.techinsights.batch.github.community.dto

import com.fasterxml.jackson.annotation.JsonProperty

// hacker news
data class HnSearchResponse(
    val hits: List<HnHit> = emptyList(),
) {
    data class HnHit(
        @JsonProperty("objectID") val objectId: String,
        val title: String?,
        val url: String?,
        val points: Int = 0,
        @JsonProperty("num_comments") val numComments: Int = 0,
        val author: String? = null,
    )

    fun top5(): List<HnHit> = hits.sortedByDescending { it.points }.take(5)
}
