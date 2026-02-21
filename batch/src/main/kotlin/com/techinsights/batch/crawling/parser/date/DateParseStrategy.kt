package com.techinsights.batch.crawling.parser.date

import java.time.LocalDateTime

fun interface DateParseStrategy {

  fun parse(dateString: String): LocalDateTime?
}