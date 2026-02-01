package com.techinsights.api.config

import com.techinsights.api.service.auth.CustomOAuth2UserService
import com.techinsights.api.service.auth.OAuth2SuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import com.techinsights.api.util.auth.JwtAuthenticationFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsProperties: CorsProperties
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(object : org.springframework.web.filter.OncePerRequestFilter() {
                override fun doFilterInternal(
                    request: jakarta.servlet.http.HttpServletRequest,
                    response: jakarta.servlet.http.HttpServletResponse,
                    filterChain: jakarta.servlet.FilterChain
                ) {
                    val method = request.method
                    if (method == "POST" || method == "PUT" || method == "DELETE" || method == "PATCH") {
                        if (request.getHeader("X-Requested-With") == null) {
                            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "CSRF protection: X-Requested-With header is missing")
                            return
                        }
                    }
                    filterChain.doFilter(request, response)
                }
            }, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/posts/**").permitAll()
                    .requestMatchers("/api/v1/companies/**").permitAll()
                    .requestMatchers("/api/v1/categories/**").permitAll()
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/recommendations/**").permitAll()
                    .requestMatchers("/login/**", "/oauth2/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { userInfo ->
                        userInfo.userService(customOAuth2UserService)
                    }
                    .successHandler(oAuth2SuccessHandler)
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint { request, response, _ ->
                    if (request.requestURI.startsWith("/api/")) {
                        response.contentType = "application/json;charset=UTF-8"
                        response.status = jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
                        response.writer.write("""{"status": false, "message": "Unauthorized"}""")
                    } else {
                        response.sendRedirect("/login")
                    }
                }
            }
            .logout { it.disable() }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = corsProperties.allowedOrigins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
