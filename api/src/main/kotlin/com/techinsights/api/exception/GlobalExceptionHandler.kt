package com.techinsights.api.exception

import com.techinsights.domain.exception.CompanyNotFoundException
import com.techinsights.domain.exception.GithubRepositoryNotFoundException
import com.techinsights.domain.exception.PostNotFoundException
import com.techinsights.domain.exception.UnauthorizedException
import com.techinsights.api.auth.AuthException
import com.techinsights.domain.exception.user.UserException
import com.techinsights.domain.enums.exception.UserErrorCode
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import jakarta.validation.ConstraintViolationException
import mu.KotlinLogging
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(UserException::class)
    fun handleUserException(
        e: UserException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "User exception occurred: code=${e.errorCode}" }

        val status = when (e.errorCode) {
            UserErrorCode.USER_NOT_FOUND -> HttpStatus.NOT_FOUND
            UserErrorCode.DUPLICATE_NICKNAME -> HttpStatus.CONFLICT
            UserErrorCode.INVALID_NICKNAME -> HttpStatus.BAD_REQUEST
            UserErrorCode.ACCOUNT_DEACTIVATED -> HttpStatus.FORBIDDEN
        }

        return ProblemDetails.response(
            status,
            e.errorCode.code,
            e.message ?: "사용자 관련 오류가 발생했습니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(
        e: UnauthorizedException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Unauthorized access: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED",
            e.message ?: "인증이 필요합니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(AuthException::class)
    fun handleAuthException(
        e: AuthException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Authentication failed: ${e.message} (${e.errorCode})" }

        return ProblemDetails.response(
            HttpStatus.UNAUTHORIZED,
            e.errorCode,
            e.message,
            extractPath(request)
        )
    }

    @ExceptionHandler(GithubRepositoryNotFoundException::class)
    fun handleGithubRepositoryNotFound(
        e: GithubRepositoryNotFoundException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Github repository not found: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.NOT_FOUND,
            "GITHUB_REPOSITORY_NOT_FOUND",
            "레포지토리를 찾을 수 없습니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(PostNotFoundException::class)
    fun handlePostNotFound(
        e: PostNotFoundException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Post not found: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.NOT_FOUND,
            "POST_NOT_FOUND",
            "게시글을 찾을 수 없습니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(CompanyNotFoundException::class)
    fun handleCompanyNotFound(
        e: CompanyNotFoundException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Company not found: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.NOT_FOUND,
            "COMPANY_NOT_FOUND",
            "회사 정보를 찾을 수 없습니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Validation failed: fields=${e.bindingResult.fieldErrors.map { it.field }}" }

        val errors = e.bindingResult.fieldErrors
            .associate { it.field to (it.defaultMessage ?: "Invalid value") }

        return ProblemDetails.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "입력값이 올바르지 않습니다.",
            extractPath(request),
            errors
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        e: ConstraintViolationException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Constraint violation: properties=${e.constraintViolations.map { it.propertyPath.toString() }}" }

        val errors = e.constraintViolations.associate {
            it.propertyPath.toString() to it.message
        }

        return ProblemDetails.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "입력값이 올바르지 않습니다.",
            extractPath(request),
            errors
        )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(
        e: HandlerMethodValidationException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Handler method validation failed: parameters=${e.allValidationResults.map { it.methodParameter.parameterName }}" }

        val errors = e.allValidationResults
            .flatMap { result ->
                result.resolvableErrors.map { error ->
                    result.methodParameter.parameterName.orEmpty() to
                        (error.defaultMessage ?: "Invalid value")
                }
            }
            .toMap()

        return ProblemDetails.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "입력값이 올바르지 않습니다.",
            extractPath(request),
            errors
        )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        e: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Invalid argument: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            e.message ?: "잘못된 요청입니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(
        e: NoResourceFoundException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Resource not found: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(RequestNotPermitted::class)
    fun handleRequestNotPermitted(
        e: RequestNotPermitted,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Rate limit exceeded: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.TOO_MANY_REQUESTS,
            "TOO_MANY_REQUESTS",
            "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
            extractPath(request)
        )
    }

    @ExceptionHandler(CallNotPermittedException::class)
    fun handleCallNotPermitted(
        e: CallNotPermittedException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Circuit breaker open: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.SERVICE_UNAVAILABLE,
            "SERVICE_UNAVAILABLE",
            "일시적으로 서비스를 이용할 수 없습니다. 잠시 후 다시 시도해주세요.",
            extractPath(request)
        )
    }

    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MissingServletRequestPartException::class,
        ServletRequestBindingException::class
    )
    fun handleMissingOrInvalidBinding(
        e: Exception,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Request binding failed: ${e.message}" }

        val errors = when (e) {
            is MissingServletRequestParameterException -> mapOf(e.parameterName to "required")
            is MissingServletRequestPartException -> mapOf(e.requestPartName to "required")
            else -> null
        }

        return ProblemDetails.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "입력값이 올바르지 않습니다.",
            extractPath(request),
            errors
        )
    }

    @ExceptionHandler(TypeMismatchException::class)
    fun handleTypeMismatch(
        e: TypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Type mismatch: property=${e.propertyName}, requiredType=${e.requiredType?.simpleName}" }

        return ProblemDetails.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "입력값이 올바르지 않습니다.",
            extractPath(request),
            e.propertyName?.let { mapOf(it to "invalid type") }
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        e: HttpMessageNotReadableException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Unreadable request body: exception=${e::class.simpleName}" }

        return ProblemDetails.response(
            HttpStatus.BAD_REQUEST,
            "INVALID_PARAMETER",
            "요청 본문을 읽을 수 없습니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        e: HttpRequestMethodNotSupportedException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Method not allowed: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "허용되지 않은 HTTP 메서드입니다.",
            extractPath(request),
            headers = e.headers
        )
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(
        e: HttpMediaTypeNotSupportedException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Unsupported media type: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "UNSUPPORTED_MEDIA_TYPE",
            "지원하지 않는 Content-Type 입니다.",
            extractPath(request),
            headers = e.headers
        )
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleMediaTypeNotAcceptable(
        e: HttpMediaTypeNotAcceptableException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "Not acceptable: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.NOT_ACCEPTABLE,
            "NOT_ACCEPTABLE",
            "요청한 응답 형식을 제공할 수 없습니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFound(
        e: NoHandlerFoundException,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.warn { "No handler found: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.NOT_FOUND,
            "NOT_FOUND",
            "요청한 리소스를 찾을 수 없습니다.",
            extractPath(request)
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        e: Exception,
        request: WebRequest
    ): ResponseEntity<ProblemDetail> {
        logger.error(e) { "Unexpected error occurred: ${e.message}" }

        return ProblemDetails.response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            extractPath(request)
        )
    }

    private fun extractPath(request: WebRequest): String {
        return request.getDescription(false).removePrefix("uri=")
    }
}
