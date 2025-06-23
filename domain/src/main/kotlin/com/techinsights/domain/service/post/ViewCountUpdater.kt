package com.techinsights.domain.service.post

interface ViewCountUpdater {
  fun incrementViewCount(postId: String)

  companion object {
    const val INCREASE_COUNT = 1
  }
}