package com.techinsights.batch.parser.content

import com.techinsights.batch.config.props.ContentSelectorProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

class ContentSelectorRegistryTest : FunSpec({

  test("도메인에 해당하는 셀렉터를 반환한다") {
    val properties = ContentSelectorProperties(
      selectors = mapOf(
        "example.com" to listOf(".content", ".article"),
        "test.com" to listOf(".main", ".post")
      ),
      defaultSelectors = listOf(".default")
    )
    val registry = ContentSelectorRegistry(properties)

    val selectors = registry.getSelectors("example.com")

    selectors shouldContainExactly listOf(".content", ".article")
  }

  test("도메인이 없으면 기본 셀렉터를 반환한다") {
    val properties = ContentSelectorProperties(
      selectors = mapOf("example.com" to listOf(".content")),
      defaultSelectors = listOf(".default", ".fallback")
    )
    val registry = ContentSelectorRegistry(properties)

    val selectors = registry.getSelectors("unknown.com")

    selectors shouldContainExactly listOf(".default", ".fallback")
  }

  test("셀렉터 맵이 비어있으면 기본 셀렉터를 반환한다") {
    val properties = ContentSelectorProperties(
      selectors = emptyMap(),
      defaultSelectors = listOf(".default")
    )
    val registry = ContentSelectorRegistry(properties)

    val selectors = registry.getSelectors("any.com")

    selectors shouldContainExactly listOf(".default")
  }

  test("여러 도메인에 대해 올바른 셀렉터를 반환한다") {
    val properties = ContentSelectorProperties(
      selectors = mapOf(
        "blog1.com" to listOf(".post"),
        "blog2.com" to listOf(".article"),
        "blog3.com" to listOf(".content")
      ),
      defaultSelectors = listOf(".default")
    )
    val registry = ContentSelectorRegistry(properties)

    registry.getSelectors("blog1.com") shouldContainExactly listOf(".post")
    registry.getSelectors("blog2.com") shouldContainExactly listOf(".article")
    registry.getSelectors("blog3.com") shouldContainExactly listOf(".content")
    registry.getSelectors("unknown.com") shouldContainExactly listOf(".default")
  }
})
