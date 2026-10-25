package com.techinsights.api.auth

import com.techinsights.domain.dto.auth.Requester
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.context.request.NativeWebRequest

import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import com.techinsights.api.util.ClientIpExtractor
import com.techinsights.domain.enums.UserRole

import com.techinsights.api.aid.AidInterceptor
import com.techinsights.api.aid.AidProperties

class RequesterResolverTest : FunSpec({
    val aidProperties = AidProperties().apply { cookie.name = "aid" }
    val resolver = RequesterResolver(aidProperties)

    test("인증된 사용자의 경우 AuthenticatedRequester를 반환한다") {
        // given
        val webRequest = mockk<NativeWebRequest>()
        val httpRequest = mockk<HttpServletRequest>()
        val securityContext = mockk<SecurityContext>()
        val authentication = mockk<Authentication>()
        val userDetails = CustomUserDetails(userId = 1L, email = "test@example.com", role = UserRole.USER)

        mockkStatic(SecurityContextHolder::class)
        every { SecurityContextHolder.getContext() } returns securityContext
        every { securityContext.authentication } returns authentication
        every { authentication.isAuthenticated } returns true
        every { authentication.principal } returns userDetails

        every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns httpRequest
        // ClientIpExtractor is an object, but we can't easily mock it without mockkObject
        // Let's just assume it works or mock it if needed.
        every { httpRequest.getHeader(any()) } returns null
        every { httpRequest.remoteAddr } returns "127.0.0.1"
        every { httpRequest.cookies } returns null
        every { httpRequest.getAttribute(AidInterceptor.AID_ATTRIBUTE) } returns null

        // when
        val result = resolver.resolveArgument(mockk(), null, webRequest, null)

        // then
        result.shouldBeInstanceOf<Requester.Authenticated>()
        (result as Requester.Authenticated).userId shouldBe 1L
        result.ip shouldBe "127.0.0.1"

        unmockkStatic(SecurityContextHolder::class)
    }

    test("익명 사용자면서 쿠키(aid)가 있는 경우 AnonymousRequester를 반환한다") {
        // given
        val webRequest = mockk<NativeWebRequest>()
        val httpRequest = mockk<HttpServletRequest>()
        val cookie = Cookie("aid", "anon-uuid-123")
        val securityContext = mockk<SecurityContext>()

        mockkStatic(SecurityContextHolder::class)
        every { SecurityContextHolder.getContext() } returns securityContext
        every { securityContext.authentication } returns null

        every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns httpRequest
        every { httpRequest.getHeader(any()) } returns null
        every { httpRequest.remoteAddr } returns "127.0.0.1"
        every { httpRequest.cookies } returns arrayOf(cookie)
        every { httpRequest.getAttribute(AidInterceptor.AID_ATTRIBUTE) } returns null

        // when
        val result = resolver.resolveArgument(mockk(), null, webRequest, null)

        // then
        result.shouldBeInstanceOf<Requester.Anonymous>()
        (result as Requester.Anonymous).anonymousId shouldBe "anon-uuid-123"
        result.ip shouldBe "127.0.0.1"
        result.carriedAidCookie shouldBe true

        unmockkStatic(SecurityContextHolder::class)
    }

    test("첫 요청(쿠키 없음)이면 인터셉터가 발급한 aid를 식별자로 사용한다") {
        val webRequest = mockk<NativeWebRequest>()
        val httpRequest = mockk<HttpServletRequest>()
        val securityContext = mockk<SecurityContext>()

        mockkStatic(SecurityContextHolder::class)
        every { SecurityContextHolder.getContext() } returns securityContext
        every { securityContext.authentication } returns null

        every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns httpRequest
        every { httpRequest.getHeader(any()) } returns null
        every { httpRequest.remoteAddr } returns "127.0.0.1"
        every { httpRequest.cookies } returns null
        every { httpRequest.getAttribute(AidInterceptor.AID_ATTRIBUTE) } returns "issued-aid-1"

        val result = resolver.resolveArgument(mockk(), null, webRequest, null)

        result.shouldBeInstanceOf<Requester.Anonymous>()
        (result as Requester.Anonymous).anonymousId shouldBe "issued-aid-1"
        result.identifier shouldBe "issued-aid-1"
        result.carriedAidCookie shouldBe false

        unmockkStatic(SecurityContextHolder::class)
    }

    test("aid가 발급되지 않은 요청(봇 등)은 IP로 대체한다") {
        val webRequest = mockk<NativeWebRequest>()
        val httpRequest = mockk<HttpServletRequest>()
        val securityContext = mockk<SecurityContext>()

        mockkStatic(SecurityContextHolder::class)
        every { SecurityContextHolder.getContext() } returns securityContext
        every { securityContext.authentication } returns null

        every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns httpRequest
        every { httpRequest.getHeader(any()) } returns null
        every { httpRequest.remoteAddr } returns "127.0.0.1"
        every { httpRequest.cookies } returns null
        every { httpRequest.getAttribute(AidInterceptor.AID_ATTRIBUTE) } returns null

        val result = resolver.resolveArgument(mockk(), null, webRequest, null)

        (result as Requester.Anonymous).anonymousId shouldBe "127.0.0.1"
        result.carriedAidCookie shouldBe false

        unmockkStatic(SecurityContextHolder::class)
    }
})
