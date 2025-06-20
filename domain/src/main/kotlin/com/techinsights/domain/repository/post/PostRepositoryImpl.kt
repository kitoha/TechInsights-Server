package com.techinsights.domain.repository.post

import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.post.Post
import com.techinsights.domain.utils.Tsid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PostRepositoryImpl(
  private val postJpaRepository: PostJpaRepository
) : PostRepository {

  override fun saveAll(posts: List<PostDto>): List<PostDto> {
    val entities = posts.map { post ->
      Post(
        id = Tsid.decode(post.id),
        title = post.title,
        url = post.url,
        content = post.content,
        publishedAt = post.publishedAt,
        thumbnail = post.thumbnail
      )
    }
    return postJpaRepository.saveAll(entities).map { entity ->
      PostDto(
        id = Tsid.encode(entity.id),
        title = entity.title,
        url = entity.url,
        content = entity.content,
        publishedAt = entity.publishedAt,
        thumbnail = entity.thumbnail
      )
    }
  }

  override fun findAllByUrlIn(urls: List<String>): List<PostDto> {
    val posts: List<Post> = postJpaRepository.findAllByUrlIn(urls)
    return posts.map { post ->
      PostDto(
        id = Tsid.encode(post.id),
        title = post.title,
        url = post.url,
        content = post.content,
        publishedAt = post.publishedAt,
        thumbnail = post.thumbnail
      )
    }
  }

  override fun getPosts(pageable: Pageable): Page<PostDto> {
    TODO()
  }
}