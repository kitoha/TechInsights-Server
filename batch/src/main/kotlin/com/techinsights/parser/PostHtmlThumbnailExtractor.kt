package com.techinsights.parser

import org.jsoup.Jsoup

object PostHtmlThumbnailExtractor : HtmlExtractor {

  override fun extractFromUrl(url: String): String? {
    return try {
      val doc = Jsoup.connect(url).get()
      val content = doc.select("article, .post-content, .article-body, #content")
      val imgs = content.select("img")
        .filterNot { img ->
          img.absUrl("src").isNullOrBlank() ||
              img.absUrl("src").contains("emoji") ||
              img.absUrl("src").contains("icon") ||
              img.absUrl("src").contains("logo") ||
              img.absUrl("src").endsWith(".svg") ||
              img.className().contains("avatar") ||
              img.attr("alt").contains("프로필") ||
              img.absUrl("src").startsWith("data:image")
        }
        .filter { img ->
          (img.attr("width").toIntOrNull() ?: 200) >= 120 &&
              (img.attr("height").toIntOrNull() ?: 200) >= 80
        }
      imgs.firstOrNull()?.absUrl("src")
    } catch (e: Exception) {
      null
    }
  }

}