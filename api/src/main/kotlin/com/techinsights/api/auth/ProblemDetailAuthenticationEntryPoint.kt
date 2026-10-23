package com.techinsights.api.auth

import com.techinsights.api.exception.ProblemDetails
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

/**
 * 401 as problem details. `code` is the reason [JwtAuthenticationFilter] rejected the access token
 * (EXPIRED_TOKEN, INVALID_TOKEN, TOKEN_TAMPERED), or UNAUTHORIZED when no token was sent at all.
 */
class ProblemDetailAuthenticationEntryPoint : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val code = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_CODE_ATTRIBUTE) as? String ?: MISSING_TOKEN_CODE
        val detail = when (code) {
            MISSING_TOKEN_CODE -> "인증이 필요한 서비스입니다."
            "EXPIRED_TOKEN" -> "토큰이 만료되었습니다."
            else -> "유효하지 않은 토큰입니다."
        }
        ProblemDetails.write(response, HttpStatus.UNAUTHORIZED, code, detail, request.requestURI)
    }

    companion object {
        const val MISSING_TOKEN_CODE = "UNAUTHORIZED"
    }
}
