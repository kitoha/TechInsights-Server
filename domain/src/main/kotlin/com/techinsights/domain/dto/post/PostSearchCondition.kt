package com.techinsights.domain.dto.post

import com.techinsights.domain.enums.PostSortType

data class PostSearchCondition(
  val sortType: PostSortType = PostSortType.RECENT,
)