package com.techinsights.batch.parser.content

class ContentSelectorRegistry {

  private val configs = listOf(
    ContentSelectorConfig(
      "techblog.woowahan.com",
      listOf(".post-content-inner > .post-content-body")
    ),
    ContentSelectorConfig(
      "tech.kakao.com",
      listOf(".inner_content > .daum-wm-content.preview")
    ),
    ContentSelectorConfig(
      "toss.tech",
      listOf("article.css-hvd0pt > div.css-1vn47db")
    ),
    ContentSelectorConfig(
      "d2.naver.com",
      listOf("article", ".post-area", ".section-content", ".post-body", ".content-area")
    ),
    ContentSelectorConfig(
      "techblog.lycorp.co.jp",
      listOf("article.bui_component > div.post_content_wrap > div.content_inner > div.content")
    ),
    ContentSelectorConfig(
      "blog.banksalad.com",
      listOf("div[class^=postDetailsstyle__PostDescription]")
    ),
    ContentSelectorConfig(
      "aws.amazon.com",
      listOf("article.blog-post section.blog-post-content[property=articleBody]")
    ),
    ContentSelectorConfig(
      "hyperconnect.github.io",
      listOf("article.post .post-content.e-content")
    ),
    ContentSelectorConfig(
      "helloworld.kurly.com",
      listOf(".post-content", ".article-body", ".post")
    ),
    ContentSelectorConfig(
      "tech.socarcorp.kr",
      listOf(".post-content", ".article-body", ".post")
    ),
    ContentSelectorConfig(
      "dev.gmarket.com",
      listOf(".post-content", ".article-body", ".post")
    ),
    ContentSelectorConfig(
      "medium.com",
      listOf("article", ".meteredContent", ".pw-post-body", ".postArticle-content")
    ),
    ContentSelectorConfig(
      "oliveyoung.tech",
      listOf("div.blog-post-content")
    )
  )

  private val defaultSelectors = listOf(
    "article", ".post-content", ".entry-content",
    "#content", ".blog-post"
  )

  fun getSelectors(domain: String): List<String> =
    configs.find { it.domain == domain }?.selectors ?: defaultSelectors
}
