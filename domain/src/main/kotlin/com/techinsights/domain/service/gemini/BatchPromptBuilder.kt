package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techinsights.domain.dto.gemini.ArticleInput
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class BatchPromptBuilder(
    @Value("classpath:prompts/batch-article-summary.md")
    private val summaryTplResource: Resource,
    @Value("classpath:prompts/batch-article-schema.json")
    private val schemaTplResource: Resource,
    @Value("classpath:prompts/batch-article-item.txt")
    private val articleItemTplResource: Resource
) {

    private val mapper = jacksonObjectMapper()

    private val summaryTemplate: String by lazy { 
        summaryTplResource.inputStream.bufferedReader().readText() 
    }
    private val schemaTemplate: String by lazy { 
        schemaTplResource.inputStream.bufferedReader().readText() 
    }
    private val articleItemTemplate: String by lazy { 
        articleItemTplResource.inputStream.bufferedReader().readText() 
    }

    fun buildPrompt(articles: List<ArticleInput>, categories: List<String>): String {
        val articlesText = articles.mapIndexed { index, article ->
            formatArticleItem(article, index + 1)
        }.joinToString(ARTICLE_SEPARATOR)

        return summaryTemplate
            .replace(PLACEHOLDER_ARTICLE_COUNT, articles.size.toString())
            .replace(PLACEHOLDER_CATEGORIES, categories.joinToString(CATEGORY_SEPARATOR))
            .replace(PLACEHOLDER_ARTICLES, articlesText)
    }

    fun buildSchema(categories: List<String>): String {
        return schemaTemplate
            .replace(PLACEHOLDER_ENUM_VALUES, mapper.writeValueAsString(categories))
    }

    private fun formatArticleItem(article: ArticleInput, index: Int): String {
        return articleItemTemplate
            .replace(PLACEHOLDER_INDEX, index.toString())
            .replace(PLACEHOLDER_ID, article.id)
            .replace(PLACEHOLDER_TITLE, article.title)
            .replace(PLACEHOLDER_CONTENT, article.content)
    }

    companion object {
        private const val PLACEHOLDER_ARTICLE_COUNT = "{{article_count}}"
        private const val PLACEHOLDER_CATEGORIES = "{{categories}}"
        private const val PLACEHOLDER_ARTICLES = "{{articles}}"
        private const val PLACEHOLDER_ENUM_VALUES = "{{enumValues}}"
        private const val PLACEHOLDER_INDEX = "{{index}}"
        private const val PLACEHOLDER_ID = "{{id}}"
        private const val PLACEHOLDER_TITLE = "{{title}}"
        private const val PLACEHOLDER_CONTENT = "{{content}}"

        private const val ARTICLE_SEPARATOR = "\n\n"
        private const val CATEGORY_SEPARATOR = ", "
    }
}
