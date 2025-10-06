package com.techinsights.batch.parser.date

import java.time.LocalDateTime

interface DateParseStrategy {

  fun parse(dateString: String): LocalDateTime?
}