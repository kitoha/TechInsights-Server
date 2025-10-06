package com.techinsights.batch.parser.content

interface ContentExtractor {

  fun extract(url: String, fallbackContent: String): String
}
