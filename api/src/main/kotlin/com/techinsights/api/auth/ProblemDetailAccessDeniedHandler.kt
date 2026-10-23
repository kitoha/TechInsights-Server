package com.techinsights.api.auth

import com.techinsights.api.exception.ProblemDetails
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler

class ProblemDetailAccessDeniedHandler : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        ProblemDetails.write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.", request.requestURI)
    }
}
