package com.techinsights.api.auth

import com.techinsights.domain.dto.auth.Requester
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import com.techinsights.api.util.ClientIpExtractor
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

import com.techinsights.api.aid.AidProperties

class RequesterResolver(
    private val aidProperties: AidProperties
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == Requester::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val httpRequest = webRequest.getNativeRequest(HttpServletRequest::class.java) ?: return null
        val clientIp = ClientIpExtractor.extract(httpRequest)
        
        val authentication = SecurityContextHolder.getContext().authentication
        
        if (authentication != null && authentication.isAuthenticated && authentication !is AnonymousAuthenticationToken) {
            val principal = authentication.principal
            if (principal is CustomUserDetails) {
                return Requester.Authenticated(userId = principal.userId, ip = clientIp)
            }
        }

        // Anonymous user - check cookie first
        val anonymousId = httpRequest.cookies?.find { it.name == aidProperties.cookie.name }?.value
            ?: clientIp // Fallback to IP if no cookie

        return Requester.Anonymous(anonymousId = anonymousId, ip = clientIp)
    }
}
