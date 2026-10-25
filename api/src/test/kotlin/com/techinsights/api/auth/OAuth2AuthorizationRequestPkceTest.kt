package com.techinsights.api.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository

/**
 * Documents why the authorization-request cookie needs no encryption: the Google client is a confidential
 * client (it has a client secret), for which Spring Security does not use PKCE, so the cookie never carries a
 * code_verifier. If this ever fails (e.g. a public client / Spring Security upgrade turns PKCE on),
 * the cookie must be encrypted, not just signed.
 */
class OAuth2AuthorizationRequestPkceTest : FunSpec({

    val registration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
        .clientId("client-id")
        .clientSecret("client-secret")
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid", "profile", "email")
        .build()
    val resolver = DeviceAwareOAuth2AuthorizationRequestResolver(InMemoryClientRegistrationRepository(registration))

    test("Google(기밀 클라이언트) 인증 요청에는 PKCE(code_verifier / code_challenge)가 없다") {
        val request = MockHttpServletRequest("GET", "/oauth2/authorization/google")
        request.serverName = "api.techinsights.shop"
        request.scheme = "https"
        request.serverPort = 443

        val authorizationRequest = requireNotNull(resolver.resolve(request))

        authorizationRequest.attributes.containsKey("code_verifier") shouldBe false
        authorizationRequest.additionalParameters.containsKey("code_challenge") shouldBe false
        authorizationRequest.additionalParameters.containsKey("code_challenge_method") shouldBe false
    }

    test("쿠키에 담기는 attributes 는 registration_id 와 OIDC nonce 뿐이다 (HttpOnly/Secure 쿠키, 사용자 본인에게만 노출)") {
        val request = MockHttpServletRequest("GET", "/oauth2/authorization/google")

        val authorizationRequest = requireNotNull(resolver.resolve(request))

        authorizationRequest.attributes.keys shouldBe setOf("registration_id", "nonce")
    }
})
