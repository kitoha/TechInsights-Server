package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class PromptTemplateEngine(
    @Value("classpath:prompts/article-summary.md")
    private val summaryTplResource: Resource,
    @Value("classpath:prompts/article-schema.json")
    private val schemaTplResource: Resource
) {
    private val mapper = jacksonObjectMapper()

    private val summaryTemplate: String by lazy {
        summaryTplResource.inputStream.bufferedReader().readText()
    }
    private val schemaTemplate: String by lazy {
        schemaTplResource.inputStream.bufferedReader().readText()
    }

    fun buildPrompt(article: String, categories: List<String>): String {
        return summaryTemplate
            .replace(PLACEHOLDER_CATEGORIES, categories.joinToString(CATEGORY_SEPARATOR))
            .replace(PLACEHOLDER_ARTICLE, article)
    }

    fun buildSchema(categories: List<String>): String {
        return schemaTemplate
            .replace(PLACEHOLDER_ENUM_VALUES, mapper.writeValueAsString(categories))
    }

    companion object {
        private const val PLACEHOLDER_CATEGORIES = "{{categories}}"
        private const val PLACEHOLDER_ARTICLE = "{{article}}"
        private const val PLACEHOLDER_ENUM_VALUES = "{{enumValues}}"
        private const val CATEGORY_SEPARATOR = "|"
    }
}