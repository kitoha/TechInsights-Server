package com.techinsights.batch.parser.date

import java.time.LocalDateTime
import java.time.ZoneId

class IsoInstantParser : DateParseStrategy {

  override fun parse(dateString: String): LocalDateTime? = runCatching {
    LocalDateTime.ofInstant(
      java.time.Instant.parse(dateString),
      ZoneId.systemDefault()
    )
  }.getOrNull()
}