package com.techinsights.batch.parser

interface HtmlExtractor {
  fun extractFromUrl(url: String): String?
}