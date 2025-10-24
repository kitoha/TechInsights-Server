package com.techinsights.batch.parser.date

import java.time.LocalDateTime

fun interface DateParseStrategy {

  fun parse(dateString: String): LocalDateTime?
}