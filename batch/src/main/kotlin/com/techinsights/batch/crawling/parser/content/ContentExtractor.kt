package com.techinsights.batch.crawling.parser.content

fun interface ContentExtractor {

  fun extract(url: String, fallbackContent: String): String
}
