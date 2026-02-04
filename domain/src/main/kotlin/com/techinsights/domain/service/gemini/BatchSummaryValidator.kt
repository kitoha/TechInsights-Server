package com.techinsights.domain.service.gemini

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import org.springframework.stereotype.Component

@Component
class BatchSummaryValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    fun validate(
        input: ArticleInput,
        result: SummaryResultWithId,
        validCategories: Set<String>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (result.id != input.id) {
            errors.add("ID mismatch: expected ${input.id}, got ${result.id}")
        }

        if (!result.success) {
            errors.add("Result marked as failed: ${result.error}")
            return ValidationResult(false, errors)
        }

        when {
            result.summary.isNullOrBlank() -> {
                errors.add("Summary is blank")
            }
            result.summary.length < 50 -> {
                errors.add("Summary too short: ${result.summary.length} chars")
            }
            result.summary.length > 5000 -> {
                errors.add("Summary too long: ${result.summary.length} chars")
            }
        }

        if (result.categories.isNullOrEmpty()) {
            errors.add("No categories provided")
        } else {
            val invalidCategories = result.categories.filterNot { it in validCategories }
            if (invalidCategories.isNotEmpty()) {
                errors.add("Invalid categories: $invalidCategories")
            }
        }

        if (result.preview.isNullOrBlank()) {
            errors.add("Preview is blank")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}
