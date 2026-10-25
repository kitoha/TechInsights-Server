package com.techinsights.api.aid

import java.util.UUID
import org.springframework.stereotype.Component

@Component
class UuidV4Generator: AidGenerator {
  override fun generate() = UUID.randomUUID().toString()
}