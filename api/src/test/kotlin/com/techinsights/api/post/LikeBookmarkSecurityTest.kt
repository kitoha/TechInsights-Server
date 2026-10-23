package com.techinsights.api.post

import com.techinsights.api.aid.AidProperties
import com.techinsights.api.auth.CookieOAuth2AuthorizationRequestRepository
import com.techinsights.api.auth.CustomOAuth2UserService
import com.techinsights.api.auth.CustomUserDetails
import com.techinsights.api.auth.JwtAuthenticationFilter
import com.techinsights.api.auth.OAuth2FailureHandler
import com.techinsights.api.auth.OAuth2SuccessHandler
import com.techinsights.api.auth.RequesterResolver
import com.techinsights.api.config.CorsProperties
import com.techinsights.api.config.SecurityConfig
import com.techinsights.api.github.GithubBookmarkController
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.service.github.GithubBookmarkService
import com.techinsights.domain.service.post.PostBookmarkService
import com.techinsights.domain.service.post.PostLikeService
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@WebMvcTest(
    controllers = [PostLikeController::class, PostBookmarkController::class, GithubBookmarkController::class],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = [
                "com\\.techinsights\\.api\\.config\\..*",
                "com\\.techinsights\\.api\\.aid\\..*"
            ]
        )
    ]
)
@Import(SecurityConfig::class, LikeBookmarkSecurityTest.SecurityTestConfig::class)
class LikeBookmarkSecurityTest(
    @Autowired val mockMvc: MockMvc
) : FunSpec() {
    override fun extensions() = listOf(SpringExtension)

    @TestConfiguration
    class SecurityTestConfig {
        @Bean
        fun corsProperties() = CorsProperties(allowedOrigins = listOf("http://localhost:3000"))

        @Bean
        fun requesterResolverConfigurer() = object : WebMvcConfigurer {
            override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
                resolvers.add(RequesterResolver(AidProperties()))
            }
        }
    }

    @MockitoBean private lateinit var postLikeService: PostLikeService
    @MockitoBean private lateinit var postBookmarkService: PostBookmarkService
    @MockitoBean private lateinit var githubBookmarkService: GithubBookmarkService
    @MockitoBean private lateinit var customOAuth2UserService: CustomOAuth2UserService
    @MockitoBean private lateinit var oAuth2SuccessHandler: OAuth2SuccessHandler
    @MockitoBean private lateinit var oAuth2FailureHandler: OAuth2FailureHandler
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var cookieOAuth2AuthorizationRequestRepository: CookieOAuth2AuthorizationRequestRepository

    init {
        beforeTest {
            doAnswer { invocation ->
                val request = invocation.getArgument<ServletRequest>(0)
                val response = invocation.getArgument<ServletResponse>(1)
                val chain = invocation.getArgument<FilterChain>(2)
                chain.doFilter(request, response)
                null
            }.`when`(jwtAuthenticationFilter).doFilter(
                any(ServletRequest::class.java),
                any(ServletResponse::class.java),
                any(FilterChain::class.java)
            )
        }

        val bookmarkPaths = listOf("/api/v1/posts/p1/bookmark", "/api/v1/github/r1/bookmark")
        val likePath = "/api/v1/posts/p1/like"
        val userDetails = CustomUserDetails(userId = 1L, email = "user@test.com", role = UserRole.USER)

        bookmarkPaths.forEach { path ->
            test("비인증 PUT $path 는 401 problem+json (UNAUTHORIZED)") {
                mockMvc.perform(put(path))
                    .andExpect(status().isUnauthorized)
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            }

            test("비인증 DELETE $path 는 401 problem+json (UNAUTHORIZED)") {
                mockMvc.perform(delete(path))
                    .andExpect(status().isUnauthorized)
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            }

            test("인증된 사용자의 PUT/DELETE $path 는 통과한다") {
                mockMvc.perform(put(path).with(user(userDetails))).andExpect(status().isOk)
                mockMvc.perform(delete(path).with(user(userDetails))).andExpect(status().isOk)
            }
        }

        // Likes are anonymous-capable by design (identity = aid cookie), so they must NOT require authentication.
        test("익명 사용자도 PUT/DELETE like 를 호출할 수 있다 (401 아님)") {
            mockMvc.perform(put(likePath)).andExpect(status().isOk)
            mockMvc.perform(delete(likePath)).andExpect(status().isOk)
        }
    }
}
