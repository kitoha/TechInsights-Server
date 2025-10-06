package com.techinsights.batch.parser.date

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Rfc1123DateParser : DateParseStrategy {

  override fun parse(dateString: String): LocalDateTime? = runCatching {
    ZonedDateTime.parse(dateString, DateTimeFormatter.RFC_1123_DATE_TIME)
      .toLocalDateTime()
  }.getOrNull()
}