package com.techinsights.dto.post

import java.time.LocalDateTime

data class PostDto(
    val id: String,
    val title: String,
    val url: String,
    val content: String,
    val publishedAt: LocalDateTime,
    var thumbnail: String? = null
){
}
