package com.techinsights.domain.dto.post

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.enums.Category
import com.techinsights.domain.enums.SummaryErrorType
import com.techinsights.domain.utils.Tsid
import java.time.LocalDateTime

data class PostDto(
    val id: String,
    val title: String,
    var preview: String?,
    val url: String,
    var content: String,
    val publishedAt: LocalDateTime,
    var thumbnail: String? = null,
    val company: CompanyDto,
    val viewCount: Long = 0L,
    var categories: Set<Category>,
    val isSummary: Boolean = false,
    var isEmbedding: Boolean = false,
    // Failure metadata (only populated for failed posts)
    var failureErrorType: SummaryErrorType? = null,
    var failureErrorMessage: String? = null,
    var failureBatchSize: Int? = null,
    var failureIsBatchFailure: Boolean? = null
){
    fun toEntity(): Post {
        return Post(
            id = Tsid.decode(id),
            title = title,
            preview = preview,
            url = url,
            content = content,
            publishedAt = publishedAt,
            thumbnail = thumbnail,
            company = company.toEntity(),
            viewCount = viewCount,
            categories = categories.toMutableSet(),
            isSummary = isSummary,
            isEmbedding = isEmbedding
        )
    }

    companion object{
        fun fromEntity(entity: Post): PostDto {
            return PostDto(
                id = Tsid.encode(entity.id),
                title = entity.title,
                preview = entity.preview,
                url = entity.url,
                content = entity.content,
                publishedAt = entity.publishedAt,
                thumbnail = entity.thumbnail,
                company = CompanyDto.fromEntity(entity.company),
                viewCount = entity.viewCount,
                categories = entity.categories.toSet(),
                isSummary = entity.isSummary,
                isEmbedding = entity.isEmbedding
            )
        }
    }
}
