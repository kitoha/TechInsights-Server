package com.techinsights.batch.parser.content

fun interface ContentExtractor {

  fun extract(url: String, fallbackContent: String): String
}
