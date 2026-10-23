package com.techinsights.batch.crawling.parser.date

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class IsoInstantParser : DateParseStrategy {

  override fun parse(dateString: String): LocalDateTime? = runCatching {
    LocalDateTime.ofInstant(
      Instant.parse(dateString),
      ZoneId.systemDefault()
    )
  }.getOrNull()
}
