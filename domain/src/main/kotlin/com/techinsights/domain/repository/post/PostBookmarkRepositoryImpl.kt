package com.techinsights.domain.repository.post

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.domain.entity.company.QCompany
import com.techinsights.domain.entity.post.PostBookmark
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.entity.post.QPostBookmark
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PostBookmarkRepositoryImpl(
    private val postBookmarkJpaRepository: PostBookmarkJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : PostBookmarkRepository {

    override fun saveAndFlush(bookmark: PostBookmark): PostBookmark =
        postBookmarkJpaRepository.saveAndFlush(bookmark)

    override fun findByPostIdAndUserId(postId: Long, userId: Long): PostBookmark? =
        postBookmarkJpaRepository.findByPostIdAndUserId(postId, userId)

    override fun deleteByPostIdAndUserId(postId: Long, userId: Long): Long =
        postBookmarkJpaRepository.deleteByPostIdAndUserId(postId, userId)

    override fun findBookmarkedPosts(userId: Long, pageable: Pageable): Page<PostDto> {
        val pb = QPostBookmark.postBookmark
        val p = QPost.post
        val company = QCompany.company

        val results = queryFactory
            .selectFrom(p)
            .leftJoin(p.company, company).fetchJoin()
            .join(pb).on(pb.postId.eq(p.id))
            .where(pb.userId.eq(userId))
            .orderBy(pb.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map { PostDto.fromEntity(it) }

        val total = queryFactory
            .select(p.id.count())
            .from(p)
            .join(pb).on(pb.postId.eq(p.id))
            .where(pb.userId.eq(userId))
            .fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }
}
