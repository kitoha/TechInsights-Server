package com.techinsights.domain.enums

enum class GeminiModelType {
  GEMINI_2_5_FLASH("gemini-2.5-flash"),
  GEMINI_EMBEDDING("gemini-embedding-001");

  private val modelName: String

  constructor(modelName: String) {
    this.modelName = modelName
  }

  fun get(): String {
    return modelName
  }
}