package com.techinsights.domain.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeminiModelTypeTest {

    @Test
    fun `GEMINI_3_FLASH should return correct model name`() {
        // given
        val modelType = GeminiModelType.GEMINI_3_FLASH

        // when
        val modelName = modelType.get()

        // then
        assertThat(modelName).isEqualTo("gemini-3-pro-preview")
    }

    @Test
    fun `GEMINI_2_5_FLASH should return correct model name`() {
        // given
        val modelType = GeminiModelType.GEMINI_2_5_FLASH

        // when
        val modelName = modelType.get()

        // then
        assertThat(modelName).isEqualTo("gemini-2.5-flash")
    }

    @Test
    fun `GEMINI_2_5_FLASH_LITE should return correct model name`() {
        // given
        val modelType = GeminiModelType.GEMINI_2_5_FLASH_LITE

        // when
        val modelName = modelType.get()

        // then
        assertThat(modelName).isEqualTo("gemini-2.5-flash-lite")
    }

    @Test
    fun `GEMINI_EMBEDDING should return correct model name`() {
        // given
        val modelType = GeminiModelType.GEMINI_EMBEDDING

        // when
        val modelName = modelType.get()

        // then
        assertThat(modelName).isEqualTo("gemini-embedding-001")
    }

    @Test
    fun `all enum values should have unique model names`() {
        // given
        val allModelTypes = GeminiModelType.entries

        // when
        val modelNames = allModelTypes.map { it.get() }

        // then
        assertThat(modelNames).hasSize(4)
        assertThat(modelNames).doesNotHaveDuplicates()
    }
}
