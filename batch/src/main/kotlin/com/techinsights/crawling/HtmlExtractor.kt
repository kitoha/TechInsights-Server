package com.techinsights.crawling

interface HtmlExtractor {
  fun extractFromUrl(url: String): String?
}