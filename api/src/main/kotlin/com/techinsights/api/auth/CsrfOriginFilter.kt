package com.techinsights.api.auth

import com.techinsights.api.config.CorsProperties
import com.techinsights.api.exception.ProblemDetails
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URI

/**
 * CSRF defence for the cookie-authenticated API.
 *
 * Auth cookies are SameSite=Lax, which already keeps browsers from sending them on cross-SITE POSTs, but a sibling
 * subdomain is same-site. So every state-changing request (anything but GET/HEAD/OPTIONS/TRACE) that a browser marks
 * with a foreign Origin (or, without Origin, a foreign Referer) must come from an origin in the CORS allow-list.
 * Requests with neither header are not browser requests riding ambient cookies and are let through.
 */
@Component
class CsrfOriginFilter(
    private val corsProperties: CorsProperties
) : OncePerRequestFilter() {

    private val log = KotlinLogging.logger {}

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        SAFE_METHODS.any { it.matches(request.method) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (isAllowed(request)) {
            filterChain.doFilter(request, response)
            return
        }

        log.warn {
            "Blocked cross-origin state-changing request: method=${request.method}, uri=${request.requestURI}, " +
                "origin=${request.getHeader(HttpHeaders.ORIGIN) ?: "-"}"
        }
        ProblemDetails.write(
            response,
            HttpStatus.FORBIDDEN,
            "CSRF_ORIGIN_NOT_ALLOWED",
            "허용되지 않은 출처의 요청입니다.",
            request.requestURI
        )
    }

    private fun isAllowed(request: HttpServletRequest): Boolean {
        val origin = request.getHeader(HttpHeaders.ORIGIN)
        if (origin != null) {
            return !CorsUtils.isCorsRequest(request) || normalize(origin) in allowedOrigins()
        }

        val referer = request.getHeader(HttpHeaders.REFERER) ?: return true
        val refererOrigin = originOf(referer) ?: return false
        return refererOrigin in allowedOrigins() || refererOrigin == originOf(request.requestURL.toString())
    }

    private fun allowedOrigins(): Set<String> = corsProperties.allowedOrigins.map(::normalize).toSet()

    private fun originOf(url: String): String? = runCatching {
        val uri = URI(url)
        if (uri.scheme == null || uri.host == null) null
        else buildString {
            append(uri.scheme.lowercase()).append("://").append(uri.host.lowercase())
            if (uri.port != -1) append(':').append(uri.port)
        }
    }.getOrNull()

    private fun normalize(origin: String): String = origin.trim().trimEnd('/').lowercase()

    companion object {
        private val SAFE_METHODS = listOf(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE)
    }
}
