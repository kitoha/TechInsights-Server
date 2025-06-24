package com.techinsights.domain.service.post

import com.querydsl.jpa.impl.JPAQueryFactory
import com.techinsights.domain.entity.post.QPost
import com.techinsights.domain.utils.Tsid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RdbViewCountUpdater (
  private val jpaQueryFactory: JPAQueryFactory
) : ViewCountUpdater {

  @Transactional
  override fun incrementViewCount(postId: String) {
    val post = QPost.post

    jpaQueryFactory.update(post)
      .set(post.viewCount, post.viewCount.add(ViewCountUpdater.INCREASE_COUNT))
      .where(post.id.eq(Tsid.decode(postId)))
      .execute()
  }
}