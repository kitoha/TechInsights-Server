package com.techinsights.parser

interface HtmlExtractor {
  fun extractFromUrl(url: String): String?
}