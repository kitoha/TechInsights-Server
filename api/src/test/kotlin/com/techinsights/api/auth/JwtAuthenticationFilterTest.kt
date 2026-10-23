package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.enums.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.jsonwebtoken.Claims
import org.slf4j.LoggerFactory
import java.util.Base64

class JwtAuthenticationFilterTest : FunSpec({
    val jwtPlugin = mockk<JwtPlugin>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
            accessTokenCookieName = "at"
        )
    )
    val filter = JwtAuthenticationFilter(jwtPlugin, authProperties)

    beforeTest {
        SecurityContextHolder.clearContext()
        clearMocks(jwtPlugin)
    }

    test("유효한 Access Token 쿠키가 있으면 SecurityContext에 인증 정보가 설정되어야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>()
        
        request.setCookies(Cookie("at", "valid-at"))
        
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns 1L
        every { claims.get("email", String::class.java) } returns "test@example.com"
        every { claims.get("role", String::class.java) } returns UserRole.USER.name
        every { claims.subject } returns "1"

        every { jwtPlugin.validateAccessToken("valid-at") } returns claims
        every { chain.doFilter(any(), any()) } just Runs

        // when
        filter.doFilter(request, response, chain)

        // then
        val auth = SecurityContextHolder.getContext().authentication
        auth shouldNotBe null
        (auth?.principal as CustomUserDetails).userId shouldBe 1L
        verify { chain.doFilter(any(), any()) }
    }

    test("Access Token 쿠키가 없으면 인증 정보를 설정하지 않아야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>()

        every { chain.doFilter(any(), any()) } just Runs

        // when
        filter.doFilter(request, response, chain)

        // then
        SecurityContextHolder.getContext().authentication shouldBe null
        verify { chain.doFilter(any(), any()) }
    }

    test("리프레시 토큰이 Access Token 쿠키에 담겨 있으면 인증하지 않고 요청을 통과시켜야 한다") {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>()
        request.setCookies(Cookie("at", "refresh-in-at"))
        every { jwtPlugin.validateAccessToken("refresh-in-at") } throws InvalidTokenException("토큰 유형이 올바르지 않습니다.")
        every { chain.doFilter(any(), any()) } just Runs

        filter.doFilter(request, response, chain)

        SecurityContextHolder.getContext().authentication shouldBe null
        verify { chain.doFilter(any(), any()) }
    }

    test("role 클레임이 없는 토큰은 예외 없이 인증하지 않아야 한다") {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>()
        request.setCookies(Cookie("at", "no-role"))
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns 1L
        every { claims.get("email", String::class.java) } returns null
        every { claims.get("role", String::class.java) } returns null
        every { jwtPlugin.validateAccessToken("no-role") } returns claims
        every { chain.doFilter(any(), any()) } just Runs

        filter.doFilter(request, response, chain)

        SecurityContextHolder.getContext().authentication shouldBe null
        verify { chain.doFilter(any(), any()) }
    }

    test("알 수 없는 role 값을 가진 토큰은 예외 없이 인증하지 않아야 한다") {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mockk<FilterChain>()
        request.setCookies(Cookie("at", "bad-role"))
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns 1L
        every { claims.get("email", String::class.java) } returns "test@example.com"
        every { claims.get("role", String::class.java) } returns "SUPERUSER"
        every { jwtPlugin.validateAccessToken("bad-role") } returns claims
        every { chain.doFilter(any(), any()) } just Runs

        filter.doFilter(request, response, chain)

        SecurityContextHolder.getContext().authentication shouldBe null
        verify { chain.doFilter(any(), any()) }
    }

    context("실패 로깅") {
        val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        val previousLevel = logger.level

        beforeTest {
            appender.list.clear()
            appender.start()
            logger.addAppender(appender)
            logger.level = Level.DEBUG
        }
        afterTest {
            logger.detachAppender(appender)
            logger.level = previousLevel
        }

        fun filterWith(token: String, failure: Throwable) {
            val request = MockHttpServletRequest()
            request.setCookies(Cookie("at", token))
            val chain = mockk<FilterChain>()
            every { jwtPlugin.validateAccessToken(token) } throws failure
            every { chain.doFilter(any(), any()) } just Runs
            filter.doFilter(request, MockHttpServletResponse(), chain)
        }

        mapOf(
            ExpiredTokenException() to "expired",
            TokenTamperedException() to "bad_signature",
            WrongTokenTypeException() to "wrong_typ",
            InvalidTokenException("형식에 맞지 않는 토큰입니다.") to "malformed"
        ).forEach { (failure, category) ->
            test("${failure::class.simpleName}는 $category 카테고리로 DEBUG 로깅되고 토큰 내용은 남기지 않아야 한다") {
                filterWith("secret.token.value", failure)

                val event = appender.list.single()
                event.level shouldBe Level.DEBUG
                event.formattedMessage.contains("category=$category") shouldBe true
                event.formattedMessage.contains("secret.token.value") shouldBe false
            }
        }

        test("예상하지 못한 예외는 WARN으로 로깅되고 토큰 내용은 남기지 않아야 한다") {
            filterWith("secret.token.value", IllegalStateException("boom secret.token.value"))

            val event = appender.list.single()
            event.level shouldBe Level.WARN
            event.formattedMessage.contains("IllegalStateException") shouldBe true
            event.formattedMessage.contains("secret.token.value") shouldBe false
        }
    }
})
