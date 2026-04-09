package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.community.CommunityAnalysisInput
import com.techinsights.domain.dto.community.CommunityPost
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class CommunityBuzzPromptBuilder(
    @Value("classpath:prompts/batch-community-summary.md")
    private val summaryTplResource: Resource,
    @Value("classpath:prompts/batch-community-schema.json")
    private val schemaTplResource: Resource,
    @Value("classpath:prompts/batch-community-item.txt")
    private val itemTplResource: Resource,
    @Value("classpath:prompts/batch-community-post-line.txt")
    private val postLineTplResource: Resource,
) {
    private val summaryTemplate: String by lazy {
        summaryTplResource.inputStream.bufferedReader().readText()
    }
    private val schemaTemplate: String by lazy {
        schemaTplResource.inputStream.bufferedReader().readText()
    }
    private val itemTemplate: String by lazy {
        itemTplResource.inputStream.bufferedReader().readText()
    }
    private val postLineTemplate: String by lazy {
        postLineTplResource.inputStream.bufferedReader().readText().trimEnd()
    }

    fun buildPrompt(items: List<CommunityAnalysisInput>): String {
        val reposText = items.mapIndexed { index, item ->
            itemTemplate
                .replace(PLACEHOLDER_INDEX, (index + 1).toString())
                .replace(PLACEHOLDER_ID, item.repoFullName)
                .replace(PLACEHOLDER_NAME, item.repoName)
                .replace(PLACEHOLDER_POSTS, formatPosts(item))
        }.joinToString(ITEM_SEPARATOR)

        return summaryTemplate
            .replace(PLACEHOLDER_REPO_COUNT, items.size.toString())
            .replace(PLACEHOLDER_REPOS, reposText)
    }

    fun buildSchema(): String = schemaTemplate

    private fun formatPosts(item: CommunityAnalysisInput): String =
        (item.hnPosts + item.redditPosts).joinToString("\n") { it.toLine() }

    private fun CommunityPost.toLine(): String = postLineTemplate
        .replace(PLACEHOLDER_PLATFORM, platform)
        .replace(PLACEHOLDER_TITLE, title)
        .replace(PLACEHOLDER_SCORE, score.toString())
        .replace(PLACEHOLDER_COMMENT_COUNT, commentCount.toString())

    companion object {
        private const val PLACEHOLDER_REPO_COUNT = "{{repo_count}}"
        private const val PLACEHOLDER_REPOS = "{{repos}}"
        private const val PLACEHOLDER_INDEX = "{{index}}"
        private const val PLACEHOLDER_ID = "{{id}}"
        private const val PLACEHOLDER_NAME = "{{name}}"
        private const val PLACEHOLDER_POSTS = "{{posts}}"
        private const val PLACEHOLDER_PLATFORM = "{{platform}}"
        private const val PLACEHOLDER_TITLE = "{{title}}"
        private const val PLACEHOLDER_SCORE = "{{score}}"
        private const val PLACEHOLDER_COMMENT_COUNT = "{{comment_count}}"
        private const val ITEM_SEPARATOR = "\n\n"
    }
}
