package com.techinsights.crawling

import com.techinsights.dto.Post
import com.techinsights.utils.Tsid
import java.time.LocalDateTime

data class RawPost(
    val title: String,
    val url: String,
    val content: String,
    val publishedAt: LocalDateTime,
    var thumbnail: String? = null,
    val categories: List<String>
){
    fun toDomain() = Post(
        id = Tsid.generate(),
        title = title,
        url = url,
        content = content,
        publishedAt = publishedAt.toString(),
        categories = categories
    )
}
