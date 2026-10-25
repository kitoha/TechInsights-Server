package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import io.kotest.assertions.throwables.shouldThrow
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

class CookieOAuth2AuthorizationRequestRepositoryTest : FunSpec({

    val cookieName = "test_oauth2_auth_request"
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray())
        ),
        oauth2 = AuthProperties.OAuth2(
            authorizationRequestCookieName = cookieName,
            authorizationRequestCookieMaxAge = Duration.ofMinutes(3),
            authorizationRequestCookieSameSite = "Strict"
        )
    )
    val repository = CookieOAuth2AuthorizationRequestRepository(authProperties)

    fun authorizationRequest(): OAuth2AuthorizationRequest {
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("client-id")
            .redirectUri("https://api.techinsights.shop/login/oauth2/code/google")
            .scopes(setOf("email", "profile"))
            .state("state|device-id")
            .build()
    }

    fun savedCookieValue(response: MockHttpServletResponse): String {
        return requireNotNull(response.getHeader("Set-Cookie"))
            .substringAfter("$cookieName=")
            .substringBefore(";")
    }

    test("OAuth2 authorization request를 쿠키에 저장하고 다시 로드해야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val authorizationRequest = authorizationRequest()

        // when
        repository.saveAuthorizationRequest(authorizationRequest, request, response)
        val cookieValue = savedCookieValue(response)
        val callbackRequest = MockHttpServletRequest()
        callbackRequest.setCookies(Cookie(cookieName, cookieValue))
        val loaded = repository.loadAuthorizationRequest(callbackRequest)

        // then
        loaded?.state shouldBe "state|device-id"
        loaded?.redirectUri shouldBe "https://api.techinsights.shop/login/oauth2/code/google"
    }

    test("X-Forwarded-Proto가 https이면 Secure 쿠키로 저장해야 한다") {
        // given
        val request = MockHttpServletRequest()
        request.addHeader("X-Forwarded-Proto", "https")
        val response = MockHttpServletResponse()

        // when
        repository.saveAuthorizationRequest(authorizationRequest(), request, response)

        // then
        val setCookie = requireNotNull(response.getHeader("Set-Cookie"))
        setCookie.contains("Secure") shouldBe true
        setCookie.contains("SameSite=Strict") shouldBe true
        setCookie.contains("HttpOnly") shouldBe true
        setCookie.contains("Max-Age=180") shouldBe true
    }

    test("authorization request 제거 시 기존 요청을 반환하고 쿠키를 삭제해야 한다") {
        // given
        val saveResponse = MockHttpServletResponse()
        repository.saveAuthorizationRequest(authorizationRequest(), MockHttpServletRequest(), saveResponse)
        val cookieValue = savedCookieValue(saveResponse)
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(cookieName, cookieValue))
        val response = MockHttpServletResponse()

        // when
        val removed = repository.removeAuthorizationRequest(request, response)

        // then
        removed?.state shouldBe "state|device-id"
        requireNotNull(response.getHeader("Set-Cookie")).contains("Max-Age=0") shouldBe true
    }

    test("서명이 변조된 쿠키는 로드하지 않아야 한다") {
        // given
        val saveResponse = MockHttpServletResponse()
        repository.saveAuthorizationRequest(authorizationRequest(), MockHttpServletRequest(), saveResponse)
        val cookieValue = savedCookieValue(saveResponse)
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(cookieName, "$cookieValue-tampered"))

        // when
        val loaded = repository.loadAuthorizationRequest(request)

        // then
        loaded shouldBe null
    }

    test("authorizationRequestCookieDomain이 설정되면 Set-Cookie에 Domain 속성이 포함되어야 한다") {
        // given
        val propertiesWithDomain = AuthProperties(
            jwt = AuthProperties.Jwt(
                secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray())
            ),
            oauth2 = AuthProperties.OAuth2(
                authorizationRequestCookieName = cookieName,
                authorizationRequestCookieDomain = "techinsights.shop"
            )
        )
        val repositoryWithDomain = CookieOAuth2AuthorizationRequestRepository(propertiesWithDomain)
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        // when
        repositoryWithDomain.saveAuthorizationRequest(authorizationRequest(), request, response)

        // then
        val setCookie = requireNotNull(response.getHeader("Set-Cookie"))
        setCookie.contains("Domain=techinsights.shop") shouldBe true
    }

    test("authorizationRequestCookieDomain이 비어있으면 Set-Cookie에 Domain 속성이 없어야 한다") {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        // when
        repository.saveAuthorizationRequest(authorizationRequest(), request, response)

        // then
        val setCookie = requireNotNull(response.getHeader("Set-Cookie"))
        setCookie.contains("Domain=") shouldBe false
    }

    fun realisticAuthorizationRequest(): OAuth2AuthorizationRequest {
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .clientId("1234567890-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com")
            .redirectUri("https://api.techinsights.shop/login/oauth2/code/google")
            .scopes(setOf("openid", "profile", "email"))
            .state("a".repeat(43) + "|" + "b".repeat(36))
            .additionalParameters(
                mapOf(
                    "nonce" to "n".repeat(43),
                    "code_challenge" to "c".repeat(43),
                    "code_challenge_method" to "S256"
                )
            )
            .attributes(
                mapOf(
                    "registration_id" to "google",
                    "code_verifier" to "v".repeat(128)
                )
            )
            .authorizationRequestUri(
                "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=x&scope=openid%20profile%20email" +
                    "&state=" + "a".repeat(80) + "&redirect_uri=https://api.techinsights.shop/login/oauth2/code/google" +
                    "&nonce=" + "n".repeat(43) + "&code_challenge=" + "c".repeat(43) + "&code_challenge_method=S256"
            )
            .build()
    }

    fun signedCookie(payloadBytes: ByteArray): String {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(
            javax.crypto.spec.SecretKeySpec(
                KeyPurpose.OAUTH_COOKIE_SIGNING.deriveKey(authProperties.jwt.secretKeyBytes),
                "HmacSHA256"
            )
        )
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
        return "$encoded." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(encoded.toByteArray()))
    }

    fun loadFrom(cookieValue: String): OAuth2AuthorizationRequest? {
        val request = MockHttpServletRequest()
        request.setCookies(Cookie(cookieName, cookieValue))
        return repository.loadAuthorizationRequest(request)
    }

    test("모든 필드(scopes, additionalParameters, attributes, authorizationRequestUri)가 왕복 후 보존되어야 한다") {
        val original = realisticAuthorizationRequest()
        val response = MockHttpServletResponse()
        repository.saveAuthorizationRequest(original, MockHttpServletRequest(), response)

        val loaded = requireNotNull(loadFrom(savedCookieValue(response)))

        loaded.authorizationUri shouldBe original.authorizationUri
        loaded.clientId shouldBe original.clientId
        loaded.redirectUri shouldBe original.redirectUri
        loaded.scopes shouldBe original.scopes
        loaded.state shouldBe original.state
        loaded.additionalParameters shouldBe original.additionalParameters
        loaded.attributes shouldBe original.attributes
        loaded.authorizationRequestUri shouldBe original.authorizationRequestUri
        loaded.grantType shouldBe AuthorizationGrantType.AUTHORIZATION_CODE
    }

    test("현실적인 요청의 Set-Cookie 헤더는 4KB보다 훨씬 작아야 한다") {
        val response = MockHttpServletResponse()
        repository.saveAuthorizationRequest(realisticAuthorizationRequest(), MockHttpServletRequest(), response)

        val setCookie = requireNotNull(response.getHeader("Set-Cookie"))
        (setCookie.length < 3000) shouldBe true
    }

    test("쿠키 페이로드는 Java 직렬화가 아니라 JSON이어야 한다") {
        val response = MockHttpServletResponse()
        repository.saveAuthorizationRequest(authorizationRequest(), MockHttpServletRequest(), response)

        val payload = String(Base64.getUrlDecoder().decode(savedCookieValue(response).substringBefore(".")))

        payload.startsWith("{") shouldBe true
    }

    test("서명은 유효하지만 Java 직렬화된 구 쿠키는 없는 것으로 취급해야 한다") {
        val bytes = java.io.ByteArrayOutputStream().also {
            java.io.ObjectOutputStream(it).use { out -> out.writeObject(authorizationRequest()) }
        }.toByteArray()

        loadFrom(signedCookie(bytes)) shouldBe null
    }

    test("서명은 유효하지만 JSON이 아닌 페이로드는 없는 것으로 취급해야 한다") {
        loadFrom(signedCookie("not json".toByteArray())) shouldBe null
    }

    test("서명은 유효하지만 필수 필드가 없는 JSON은 없는 것으로 취급해야 한다") {
        loadFrom(signedCookie("""{"state":"x"}""".toByteArray())) shouldBe null
    }

    test("서명이 유효하지 않으면 페이로드를 파싱하지 않고 없는 것으로 취급해야 한다") {
        val valid = signedCookie("""{"authorizationUri":"https://a","clientId":"c","scopes":[],"additionalParameters":{},"attributes":{},"issuedAt":${Instant.now().epochSecond}}""".toByteArray())

        loadFrom(valid.substringBeforeLast(".") + ".AAAA") shouldBe null
        loadFrom(valid)?.clientId shouldBe "c"
    }

    context("문자열이 아닌 attribute / additionalParameter 값") {
        fun requestWith(attributes: Map<String, Any>, additionalParameters: Map<String, Any>) =
            OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("client-id")
                .redirectUri("https://api.techinsights.shop/login/oauth2/code/google")
                .state("state")
                .attributes(attributes)
                .additionalParameters(additionalParameters)
                .build()

        test("문자열이 아닌 attribute 값은 toString 으로 바꿔 저장하지 않고 실패한다 (조용한 손상 방지)") {
            val response = MockHttpServletResponse()

            shouldThrow<IllegalArgumentException> {
                repository.saveAuthorizationRequest(
                    requestWith(mapOf("registration_id" to "google", "count" to 1), emptyMap()),
                    MockHttpServletRequest(),
                    response
                )
            }

            response.getHeader("Set-Cookie") shouldBe null
        }

        test("문자열이 아닌 additionalParameter 값도 저장하지 않고 실패한다") {
            shouldThrow<IllegalArgumentException> {
                repository.saveAuthorizationRequest(
                    requestWith(emptyMap(), mapOf("prompt" to listOf("a", "b"))),
                    MockHttpServletRequest(),
                    MockHttpServletResponse()
                )
            }
        }

        test("서명된 쿠키 안의 숫자 값은 문자열로 강제 변환하지 않고 파싱 불가 쿠키로 취급한다") {
            val value = signedCookie(
                """{"authorizationUri":"https://a","clientId":"c","scopes":[],"additionalParameters":{},"attributes":{"count":1}}""".toByteArray()
            )

            loadFrom(value) shouldBe null
        }

        test("문자열 값만 있는 요청은 그대로 왕복한다") {
            val response = MockHttpServletResponse()
            repository.saveAuthorizationRequest(
                requestWith(mapOf("registration_id" to "google"), mapOf("nonce" to "abc")),
                MockHttpServletRequest(),
                response
            )

            val loaded = requireNotNull(loadFrom(savedCookieValue(response)))

            loaded.attributes shouldBe mapOf("registration_id" to "google")
            loaded.additionalParameters shouldBe mapOf("nonce" to "abc")
        }
    }

    context("발급 시각 (서명된 페이로드 안, 쿠키 Max-Age 와 별개로 서버가 검증)") {
        fun repositoryAt(offset: Duration) =
            CookieOAuth2AuthorizationRequestRepository(authProperties, Clock.offset(Clock.systemUTC(), offset))

        fun cookieOf(repo: CookieOAuth2AuthorizationRequestRepository): String {
            val response = MockHttpServletResponse()
            repo.saveAuthorizationRequest(authorizationRequest(), MockHttpServletRequest(), response)
            return savedCookieValue(response)
        }

        fun load(repo: CookieOAuth2AuthorizationRequestRepository, value: String): OAuth2AuthorizationRequest? {
            val request = MockHttpServletRequest()
            request.setCookies(Cookie(cookieName, value))
            return repo.loadAuthorizationRequest(request)
        }

        test("Max-Age(3분) 안에서는 로드된다") {
            val value = cookieOf(repository)

            load(repositoryAt(Duration.ofMinutes(2)), value)?.state shouldBe "state|device-id"
        }

        test("복사해 둔 쿠키라도 Max-Age 가 지나면 서버가 거부한다") {
            val value = cookieOf(repository)

            load(repositoryAt(Duration.ofMinutes(10)), value) shouldBe null
        }

        test("미래 시각으로 발급된 쿠키는 거부한다") {
            val future = Instant.now().plusSeconds(3600).epochSecond
            val value = signedCookie(
                """{"authorizationUri":"https://a","clientId":"c","scopes":[],"additionalParameters":{},"attributes":{},"issuedAt":$future}""".toByteArray()
            )

            load(repository, value) shouldBe null
        }

        test("발급 시각이 없는 쿠키는 파싱 실패가 아니라 만료로 취급해 거부하고, 값 없이 전용 로그를 남긴다") {
            val logger = LoggerFactory.getLogger(CookieOAuth2AuthorizationRequestRepository::class.java) as Logger
            val appender = ListAppender<ILoggingEvent>().also { it.start() }
            logger.addAppender(appender)
            try {
                val value = signedCookie(
                    """{"authorizationUri":"https://a","clientId":"c","scopes":[],"additionalParameters":{},"attributes":{},"state":"secret-state-value"}""".toByteArray()
                )

                load(repository, value) shouldBe null

                val message = appender.list.single { it.formattedMessage.contains("issuedAt") }.formattedMessage
                message.contains("expired") shouldBe true
                message.contains("could not be parsed") shouldBe false
                message.contains("secret-state-value") shouldBe false
                message.contains(value) shouldBe false
            } finally {
                logger.detachAppender(appender)
            }
        }

        test("발급 시각이 있는 정상 쿠키는 계속 로드된다 (필드는 선택 입력이지만 항상 새로 기록한다)") {
            val value = cookieOf(repository)

            load(repository, value)?.state shouldBe "state|device-id"
        }
    }
})
