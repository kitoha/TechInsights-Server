package com.techinsights.batch.crawling.parser.date

import java.time.LocalDateTime

class CompositeDateParser(
  private val parsers: List<DateParseStrategy> = listOf(
    Rfc1123DateParser(),
    IsoDateTimeParser(),
    IsoInstantParser()
  )
) {

  fun parse(dateString: String?): LocalDateTime {
    if (dateString.isNullOrBlank()) return LocalDateTime.now()

    return parsers.firstNotNullOfOrNull { it.parse(dateString) }
      ?: LocalDateTime.now()
  }
}

