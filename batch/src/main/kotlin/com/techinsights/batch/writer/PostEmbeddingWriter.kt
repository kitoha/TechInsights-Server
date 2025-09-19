package com.techinsights.batch.writer

import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.entity.post.PostEmbedding
import com.techinsights.domain.repository.post.PostEmbeddingJpaRepository
import com.techinsights.domain.repository.post.PostRepository
import com.techinsights.domain.utils.Tsid
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class PostEmbeddingWriter(
    private val postEmbeddingJpaRepository: PostEmbeddingJpaRepository,
    private val postRepository: PostRepository
) : ItemWriter<PostEmbeddingDto?> {

    private val log = LoggerFactory.getLogger(PostEmbeddingWriter::class.java)

    override fun write(chunk: Chunk<out PostEmbeddingDto>) {
        val items = chunk.items
        if (items.isNotEmpty()) {
            val filteredItems = items.filterNotNull()
            val embeddings = filteredItems.map { dto ->
                PostEmbedding(
                    postId = Tsid.decode(dto.postId),
                    companyName = dto.companyName,
                    categories = dto.categories,
                    content = dto.content,
                    embeddingVector = dto.embeddingVector
                )
            }
            postEmbeddingJpaRepository.saveAll(embeddings)

            val postIds = filteredItems.map { it.postId }
            val posts = postRepository.findAllByIdIn(postIds)
            posts.forEach { it.isEmbedding = true }
            postRepository.saveAll(posts)
            log.info("Successfully saved embeddings posts")
        }
    }
}
