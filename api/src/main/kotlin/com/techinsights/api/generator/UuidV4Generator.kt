package com.techinsights.api.generator

import org.springframework.stereotype.Component

@Component
class UuidV4Generator: AidGenerator {
  override fun generate() = java.util.UUID.randomUUID().toString()
}