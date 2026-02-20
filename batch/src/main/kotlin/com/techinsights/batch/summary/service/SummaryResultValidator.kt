package com.techinsights.batch.summary.service

import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import com.techinsights.domain.enums.Category
import com.techinsights.domain.service.gemini.BatchSummaryValidator
import org.springframework.stereotype.Component

@Component
class SummaryResultValidator(
    private val batchValidator: BatchSummaryValidator
) {
    
    private val validCategories = Category.entries.map { it.name }.toSet()
    
    fun validate(
        postId: String,
        postTitle: String,
        postContent: String,
        result: SummaryResultWithId
    ): ValidationResult {
        val validation = batchValidator.validate(
            ArticleInput(postId, postTitle, postContent),
            result,
            validCategories
        )
        
        return ValidationResult(
            isValid = validation.isValid,
            errors = validation.errors
        )
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )
}
