package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.enums.UserRole
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtPlugin: JwtPlugin,
    private val authProperties: AuthProperties
) : OncePerRequestFilter() {

    private val log = KotlinLogging.logger {}

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null) {
            try {
                val claims = jwtPlugin.validateAccessToken(token)
                val userId = claims.get("userId", Long::class.javaObjectType)
                val email = claims.get("email", String::class.java)
                val roleStr = claims.get("role", String::class.java)
                val role = UserRole.valueOf(roleStr)

                val userDetails = CustomUserDetails(
                    userId = userId,
                    email = email,
                    role = role,
                    attributes = emptyMap()
                )

                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )

                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: AuthException) {
                SecurityContextHolder.clearContext()
                log.debug { "JWT authentication failed: uri=${request.requestURI}, category=${category(e)}" }
            } catch (e: Exception) {
                SecurityContextHolder.clearContext()
                log.warn { "JWT authentication failed unexpectedly: uri=${request.requestURI}, exception=${e::class.simpleName}" }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun category(e: AuthException): String = when (e) {
        is ExpiredTokenException -> "expired"
        is TokenTamperedException -> "bad_signature"
        is WrongTokenTypeException -> "wrong_typ"
        is InvalidTokenException -> "malformed"
        is UnauthorizedException -> "unauthorized"
    }

    private fun extractToken(request: HttpServletRequest): String? {
        return request.cookies?.find { it.name == authProperties.jwt.accessTokenCookieName }?.value
    }
}
