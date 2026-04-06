package com.techinsights.api.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository

class DeviceAwareOAuth2AuthorizationRequestResolverTest : FunSpec({
    val clientRegistrationRepository = mockk<ClientRegistrationRepository>()
    val resolver = DeviceAwareOAuth2AuthorizationRequestResolver(clientRegistrationRepository)

    test("deviceId 파라미터가 있으면 state에 '|deviceId' 형식으로 추가되어야 한다") {
        val request = MockHttpServletRequest().apply {
            servletPath = "/oauth2/authorization/google"
            addParameter("deviceId", "test-device-uuid")
        }
        val baseState = "csrf-token-abc"
        val result = resolver.appendDeviceId(baseState, request)
        result shouldContain "|test-device-uuid"
        result.substringAfter("|") shouldBe "test-device-uuid"
    }

    test("deviceId 파라미터가 없으면 state가 변경되지 않아야 한다") {
        val request = MockHttpServletRequest().apply {
            servletPath = "/oauth2/authorization/google"
        }
        val baseState = "csrf-token-abc"
        val result = resolver.appendDeviceId(baseState, request)
        result shouldBe baseState
    }

    test("state에서 deviceId를 올바르게 추출해야 한다") {
        val state = "csrf-token-abc|my-device-id"
        val deviceId = DeviceAwareOAuth2AuthorizationRequestResolver.extractDeviceId(state)
        deviceId shouldBe "my-device-id"
    }

    test("state에 deviceId가 없으면 null을 반환해야 한다") {
        val state = "csrf-token-abc"
        val deviceId = DeviceAwareOAuth2AuthorizationRequestResolver.extractDeviceId(state)
        deviceId.shouldBeNull()
    }

    test("빈 deviceId는 null로 처리해야 한다") {
        val state = "csrf-token-abc|"
        val deviceId = DeviceAwareOAuth2AuthorizationRequestResolver.extractDeviceId(state)
        deviceId.shouldBeNull()
    }

    test("128자를 초과하는 deviceId는 무시되고 state가 변경되지 않아야 한다") {
        val request = MockHttpServletRequest().apply {
            addParameter("deviceId", "a".repeat(129))
        }
        val baseState = "csrf-token-abc"
        val result = resolver.appendDeviceId(baseState, request)
        result shouldBe baseState
    }

    test("허용되지 않는 문자가 포함된 deviceId는 무시되고 state가 변경되지 않아야 한다") {
        val request = MockHttpServletRequest().apply {
            addParameter("deviceId", "uuid|injected")
        }
        val baseState = "csrf-token-abc"
        val result = resolver.appendDeviceId(baseState, request)
        result shouldBe baseState
    }

    test("허용되지 않는 특수문자가 포함된 deviceId는 무시되어야 한다") {
        val request = MockHttpServletRequest().apply {
            addParameter("deviceId", "<script>alert(1)</script>")
        }
        val baseState = "csrf-token-abc"
        val result = resolver.appendDeviceId(baseState, request)
        result shouldBe baseState
    }

    test("정확히 128자인 deviceId는 허용되어야 한다") {
        val validDeviceId = "a".repeat(128)
        val request = MockHttpServletRequest().apply {
            addParameter("deviceId", validDeviceId)
        }
        val baseState = "csrf-token-abc"
        val result = resolver.appendDeviceId(baseState, request)
        result shouldBe "csrf-token-abc|$validDeviceId"
    }

    test("영문/숫자/하이픈/언더스코어로 구성된 UUID 형식 deviceId는 허용되어야 한다") {
        val request = MockHttpServletRequest().apply {
            addParameter("deviceId", "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        }
        val baseState = "csrf-token-abc"
        val result = resolver.appendDeviceId(baseState, request)
        result shouldBe "csrf-token-abc|a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    }
})
