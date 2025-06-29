package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class PromptTemplateEngine(
  @Value("classpath:prompts/article-summary.md")
  private val summaryTpl: Resource,
  @Value("classpath:prompts/article-schema.json")
  private val schemaTpl: Resource
){
  private val mapper = jacksonObjectMapper()

  fun buildPrompt(article: String, categories: List<String>): String =
    summaryTpl.inputStream.bufferedReader().readText()
      .replace("{{categories}}", categories.joinToString("|"))
      .replace("{{article}}", article)

  fun buildSchema(categories: List<String>): String =
    schemaTpl.inputStream.bufferedReader().readText()
      .replace("{{enumValues}}", mapper.writeValueAsString(categories))

}