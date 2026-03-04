package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class GithubReadmePromptBuilder(
    @Value("classpath:prompts/batch-readme-summary.md")
    private val summaryTplResource: Resource,
    @Value("classpath:prompts/batch-readme-schema.json")
    private val schemaTplResource: Resource,
    @Value("classpath:prompts/batch-readme-item.txt")
    private val readmeItemTplResource: Resource,
) {
    private val summaryTemplate: String by lazy {
        summaryTplResource.inputStream.bufferedReader().readText()
    }
    private val schemaTemplate: String by lazy {
        schemaTplResource.inputStream.bufferedReader().readText()
    }
    private val readmeItemTemplate: String by lazy {
        readmeItemTplResource.inputStream.bufferedReader().readText()
    }

    fun buildPrompt(items: List<ArticleInput>): String {
        val reposText = items.mapIndexed { index, item ->
            readmeItemTemplate
                .replace(PLACEHOLDER_INDEX, (index + 1).toString())
                .replace(PLACEHOLDER_ID, item.id)
                .replace(PLACEHOLDER_TITLE, item.title)
                .replace(PLACEHOLDER_CONTENT, item.content)
        }.joinToString(ITEM_SEPARATOR)

        return summaryTemplate
            .replace(PLACEHOLDER_REPO_COUNT, items.size.toString())
            .replace(PLACEHOLDER_REPOS, reposText)
    }

    fun buildSchema(): String = schemaTemplate

    companion object {
        private const val PLACEHOLDER_REPO_COUNT = "{{repo_count}}"
        private const val PLACEHOLDER_REPOS = "{{repos}}"
        private const val PLACEHOLDER_INDEX = "{{index}}"
        private const val PLACEHOLDER_ID = "{{id}}"
        private const val PLACEHOLDER_TITLE = "{{title}}"
        private const val PLACEHOLDER_CONTENT = "{{content}}"
        private const val ITEM_SEPARATOR = "\n\n"
    }
}
