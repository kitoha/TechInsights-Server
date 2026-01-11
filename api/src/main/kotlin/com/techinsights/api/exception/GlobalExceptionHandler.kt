package com.techinsights.api.exception

import com.techinsights.domain.exception.CompanyNotFoundException
import com.techinsights.domain.exception.PostNotFoundException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(PostNotFoundException::class)
    fun handlePostNotFound(
        e: PostNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Post not found: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    errorCode = "POST_NOT_FOUND",
                    message = "게시글을 찾을 수 없습니다.",
                    path = extractPath(request)
                )
            )
    }

    @ExceptionHandler(CompanyNotFoundException::class)
    fun handleCompanyNotFound(
        e: CompanyNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Company not found: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    errorCode = "COMPANY_NOT_FOUND",
                    message = "회사 정보를 찾을 수 없습니다.",
                    path = extractPath(request)
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Validation failed: ${e.bindingResult.allErrors}" }

        val errors = e.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "Invalid value") }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    errorCode = "INVALID_PARAMETER",
                    message = "입력값이 올바르지 않습니다.",
                    path = extractPath(request),
                    details = errors
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        e: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Invalid argument: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    errorCode = "INVALID_PARAMETER",
                    message = e.message ?: "잘못된 요청입니다.",
                    path = extractPath(request)
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        e: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(e) { "Unexpected error occurred: ${e.message}" }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    errorCode = "INTERNAL_SERVER_ERROR",
                    message = "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                    path = extractPath(request)
                )
            )
    }

    private fun extractPath(request: WebRequest): String {
        return request.getDescription(false).removePrefix("uri=")
    }
}
