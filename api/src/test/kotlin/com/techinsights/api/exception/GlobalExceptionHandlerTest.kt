package com.techinsights.api.exception

import com.techinsights.api.auth.ExpiredTokenException
import com.techinsights.api.auth.InvalidTokenException
import com.techinsights.api.auth.TokenTamperedException
import com.techinsights.api.auth.UnauthorizedException
import com.techinsights.domain.exception.CompanyNotFoundException
import com.techinsights.domain.exception.user.DuplicateNicknameException
import com.techinsights.domain.exception.user.InvalidNicknameException
import com.techinsights.domain.exception.user.UserNotFoundException
import com.techinsights.domain.exception.PostNotFoundException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest

class GlobalExceptionHandlerTest : FunSpec({

    val handler = GlobalExceptionHandler()
    val mockRequest = mockk<WebRequest>()

    beforeTest {
        every { mockRequest.getDescription(false) } returns "uri=/api/test"
    }

    test("handleUserException - USER_NOT_FOUND should return 404") {
        // given
        val exception = UserNotFoundException(1L)

        // when
        val response = handler.handleUserException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.NOT_FOUND
        response.body?.errorCode shouldBe "USER_001"
        response.body?.path shouldBe "/api/test"
    }

    test("handleUserException - DUPLICATE_NICKNAME should return 409") {
        // given
        val exception = DuplicateNicknameException("testnick")

        // when
        val response = handler.handleUserException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.CONFLICT
        response.body?.errorCode shouldBe "USER_002"
        response.body?.message shouldBe "이미 사용 중인 닉네임입니다. (닉네임: testnick)"
    }

    test("handleUserException - INVALID_NICKNAME should return 400") {
        // given
        val exception = InvalidNicknameException("bad nick", "특수문자 포함")

        // when
        val response = handler.handleUserException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.errorCode shouldBe "USER_003"
        response.body?.message shouldBe "유효하지 않은 닉네임입니다. (닉네임: bad nick, 사유: 특수문자 포함)"
    }

    test("handleAuthException - ExpiredTokenException should return 401") {
        // given
        val exception = ExpiredTokenException()

        // when
        val response = handler.handleAuthException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.body?.errorCode shouldBe "EXPIRED_TOKEN"
        response.body?.message shouldBe "토큰이 만료되었습니다."
        response.body?.path shouldBe "/api/test"
    }

    test("handleAuthException - InvalidTokenException should return 401") {
        // given
        val exception = InvalidTokenException("잘못된 토큰 형식")

        // when
        val response = handler.handleAuthException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.body?.errorCode shouldBe "INVALID_TOKEN"
        response.body?.message shouldBe "잘못된 토큰 형식"
        response.body?.path shouldBe "/api/test"
    }

    test("handleAuthException - TokenTamperedException should return 401") {
        // given
        val exception = TokenTamperedException()

        // when
        val response = handler.handleAuthException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.body?.errorCode shouldBe "TOKEN_TAMPERED"
        response.body?.message shouldBe "토큰이 변조되었습니다."
    }

    test("handleAuthException - UnauthorizedException should return 401") {
        // given
        val exception = UnauthorizedException()

        // when
        val response = handler.handleAuthException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.body?.errorCode shouldBe "UNAUTHORIZED"
        response.body?.message shouldBe "인증이 필요한 서비스입니다."
    }

    test("handlePostNotFound should return 404 with POST_NOT_FOUND error code") {
        // given
        val exception = PostNotFoundException("Post with ID 123 not found")

        // when
        val response = handler.handlePostNotFound(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.NOT_FOUND
        response.body shouldNotBe null
        response.body?.errorCode shouldBe "POST_NOT_FOUND"
        response.body?.message shouldBe "게시글을 찾을 수 없습니다."
        response.body?.path shouldBe "/api/test"
        response.body?.details shouldBe null
    }

    test("handlePostNotFound should not expose internal exception message") {
        // given
        val internalMessage = "Internal database error: connection timeout"
        val exception = PostNotFoundException(internalMessage)

        // when
        val response = handler.handlePostNotFound(exception, mockRequest)

        // then
        response.body?.message shouldBe "게시글을 찾을 수 없습니다."
        response.body?.message shouldNotBe internalMessage
    }

    test("handleCompanyNotFound should return 404 with COMPANY_NOT_FOUND error code") {
        // given
        val exception = CompanyNotFoundException("Company with ID 456 not found")

        // when
        val response = handler.handleCompanyNotFound(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.NOT_FOUND
        response.body shouldNotBe null
        response.body?.errorCode shouldBe "COMPANY_NOT_FOUND"
        response.body?.message shouldBe "회사 정보를 찾을 수 없습니다."
        response.body?.path shouldBe "/api/test"
        response.body?.details shouldBe null
    }

    test("handleCompanyNotFound should not expose internal exception message") {
        // given
        val internalMessage = "Database constraint violation"
        val exception = CompanyNotFoundException(internalMessage)

        // when
        val response = handler.handleCompanyNotFound(exception, mockRequest)

        // then
        response.body?.message shouldBe "회사 정보를 찾을 수 없습니다."
        response.body?.message shouldNotBe internalMessage
    }

    test("handleValidationException should return 400 with field errors in details") {
        // given
        val bindingResult = mockk<BindingResult>()
        val fieldErrors = listOf(
            FieldError("target", "email", "must be valid email"),
            FieldError("target", "name", "must not be blank")
        )
        every { bindingResult.allErrors } returns fieldErrors
        every { bindingResult.fieldErrors } returns fieldErrors

        val exception = mockk<MethodArgumentNotValidException>()
        every { exception.bindingResult } returns bindingResult

        // when
        val response = handler.handleValidationException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body shouldNotBe null
        response.body?.errorCode shouldBe "INVALID_PARAMETER"
        response.body?.message shouldBe "입력값이 올바르지 않습니다."
        response.body?.path shouldBe "/api/test"
        response.body?.details shouldNotBe null
        response.body?.details?.get("email") shouldBe "must be valid email"
        response.body?.details?.get("name") shouldBe "must not be blank"
    }

    test("handleValidationException should handle null default message") {
        // given
        val bindingResult = mockk<BindingResult>()
        val fieldErrors = listOf(
            FieldError("target", "field1", null, false, null, null, null)
        )
        every { bindingResult.allErrors } returns fieldErrors
        every { bindingResult.fieldErrors } returns fieldErrors

        val exception = mockk<MethodArgumentNotValidException>()
        every { exception.bindingResult } returns bindingResult

        // when
        val response = handler.handleValidationException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.details?.get("field1") shouldBe "Invalid value"
    }

    test("handleValidationException should handle empty field errors") {
        // given
        val bindingResult = mockk<BindingResult>()
        every { bindingResult.allErrors } returns emptyList()
        every { bindingResult.fieldErrors } returns emptyList()

        val exception = mockk<MethodArgumentNotValidException>()
        every { exception.bindingResult } returns bindingResult

        // when
        val response = handler.handleValidationException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.errorCode shouldBe "INVALID_PARAMETER"
        response.body?.details shouldNotBe null
        response.body?.details?.isEmpty() shouldBe true
    }

    test("handleIllegalArgument should return 400 with INVALID_PARAMETER error code") {
        // given
        val exception = IllegalArgumentException("Invalid parameter value")

        // when
        val response = handler.handleIllegalArgument(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body shouldNotBe null
        response.body?.errorCode shouldBe "INVALID_PARAMETER"
        response.body?.message shouldBe "Invalid parameter value"
        response.body?.path shouldBe "/api/test"
        response.body?.details shouldBe null
    }

    test("handleIllegalArgument should use default message when exception message is null") {
        // given
        val exception = IllegalArgumentException(null as String?)

        // when
        val response = handler.handleIllegalArgument(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.BAD_REQUEST
        response.body?.message shouldBe "잘못된 요청입니다."
    }

    test("handleIllegalArgument should preserve exception message") {
        // given
        val customMessage = "Custom validation error"
        val exception = IllegalArgumentException(customMessage)

        // when
        val response = handler.handleIllegalArgument(exception, mockRequest)

        // then
        response.body?.message shouldBe customMessage
    }

    test("handleGenericException should return 500 with INTERNAL_SERVER_ERROR") {
        // given
        val exception = RuntimeException("Unexpected error occurred")

        // when
        val response = handler.handleGenericException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        response.body shouldNotBe null
        response.body?.errorCode shouldBe "INTERNAL_SERVER_ERROR"
        response.body?.message shouldBe "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        response.body?.path shouldBe "/api/test"
        response.body?.details shouldBe null
    }

    test("handleGenericException should not expose internal exception message") {
        // given
        val internalError = "NullPointerException at line 123 in DatabaseService.kt"
        val exception = RuntimeException(internalError)

        // when
        val response = handler.handleGenericException(exception, mockRequest)

        // then
        response.body?.message shouldBe "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
        response.body?.message shouldNotBe internalError
    }

    test("handleGenericException should handle NullPointerException") {
        // given
        val exception = NullPointerException("Object was null")

        // when
        val response = handler.handleGenericException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        response.body?.errorCode shouldBe "INTERNAL_SERVER_ERROR"
        response.body?.message shouldBe "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
    }

    test("handleGenericException should handle custom RuntimeException") {
        // given
        class CustomException(message: String) : RuntimeException(message)
        val exception = CustomException("Custom error")

        // when
        val response = handler.handleGenericException(exception, mockRequest)

        // then
        response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        response.body?.errorCode shouldBe "INTERNAL_SERVER_ERROR"
    }

    test("extractPath should remove 'uri=' prefix from request description") {
        // given
        every { mockRequest.getDescription(false) } returns "uri=/api/posts/123"

        // when
        val exception = PostNotFoundException("test")
        val response = handler.handlePostNotFound(exception, mockRequest)

        // then
        response.body?.path shouldBe "/api/posts/123"
    }

    test("extractPath should handle path without prefix") {
        // given
        every { mockRequest.getDescription(false) } returns "/api/companies"

        // when
        val exception = CompanyNotFoundException("test")
        val response = handler.handleCompanyNotFound(exception, mockRequest)

        // then
        response.body?.path shouldBe "/api/companies"
    }

    test("extractPath should handle complex paths with query parameters") {
        // given
        every { mockRequest.getDescription(false) } returns "uri=/api/posts?page=1&size=10"

        // when
        val exception = PostNotFoundException("test")
        val response = handler.handlePostNotFound(exception, mockRequest)

        // then
        response.body?.path shouldBe "/api/posts?page=1&size=10"
    }

    test("all exception handlers should set timestamp") {
        // given
        val exception = PostNotFoundException("test")

        // when
        val response = handler.handlePostNotFound(exception, mockRequest)

        // then
        response.body?.timestamp shouldNotBe null
    }

    test("multiple validation errors should all be included in details") {
        // given
        val bindingResult = mockk<BindingResult>()
        val fieldErrors = listOf(
            FieldError("user", "email", "invalid email format"),
            FieldError("user", "password", "too short"),
            FieldError("user", "username", "already exists"),
            FieldError("user", "age", "must be positive")
        )
        every { bindingResult.allErrors } returns fieldErrors
        every { bindingResult.fieldErrors } returns fieldErrors

        val exception = mockk<MethodArgumentNotValidException>()
        every { exception.bindingResult } returns bindingResult

        // when
        val response = handler.handleValidationException(exception, mockRequest)

        // then
        response.body?.details?.size shouldBe 4
        response.body?.details?.get("email") shouldBe "invalid email format"
        response.body?.details?.get("password") shouldBe "too short"
        response.body?.details?.get("username") shouldBe "already exists"
        response.body?.details?.get("age") shouldBe "must be positive"
    }

    test("exception handlers should work with different request paths") {
        // given
        val paths = listOf(
            "uri=/api/v1/posts/123",
            "uri=/api/v1/companies",
            "uri=/api/v1/search?query=test",
            "uri=/health"
        )

        paths.forEach { path ->
            every { mockRequest.getDescription(false) } returns path
            val exception = PostNotFoundException("test")

            // when
            val response = handler.handlePostNotFound(exception, mockRequest)

            // then
            response.body?.path shouldBe path.removePrefix("uri=")
        }
    }
})
