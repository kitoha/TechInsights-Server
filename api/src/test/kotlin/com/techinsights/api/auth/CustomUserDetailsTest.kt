package com.techinsights.api.auth

import com.techinsights.domain.enums.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.security.core.authority.SimpleGrantedAuthority

class CustomUserDetailsTest : FunSpec({

    test("CustomUserDetails 생성 - 기본 필드") {
        // when
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // then
        userDetails.userId shouldBe 1L
        userDetails.email shouldBe "test@example.com"
        userDetails.role shouldBe UserRole.USER
    }

    test("CustomUserDetails - OAuth2 attributes 포함") {
        // given
        val attributes = mapOf(
            "sub" to "google-123",
            "name" to "Test User",
            "picture" to "https://example.com/profile.png"
        )

        // when
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER,
            attributes = attributes
        )

        // then
        userDetails.getAttributes() shouldBe attributes
        userDetails.getAttributes()["sub"] shouldBe "google-123"
        userDetails.getAttributes()["name"] shouldBe "Test User"
    }

    test("CustomUserDetails - attributes가 빈 맵인 경우") {
        // when
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // then
        userDetails.getAttributes() shouldBe emptyMap()
    }

    test("getName은 email을 반환") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "user@example.com",
            role = UserRole.USER
        )

        // when & then
        userDetails.getName() shouldBe "user@example.com"
    }

    test("getAuthorities - USER 권한") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when
        val authorities = userDetails.getAuthorities()

        // then
        authorities shouldHaveSize 1
        authorities shouldContain SimpleGrantedAuthority("ROLE_USER")
    }

    test("getAuthorities - ADMIN 권한") {
        // given
        val userDetails = CustomUserDetails(
            userId = 2L,
            email = "admin@example.com",
            role = UserRole.ADMIN
        )

        // when
        val authorities = userDetails.getAuthorities()

        // then
        authorities shouldHaveSize 1
        authorities shouldContain SimpleGrantedAuthority("ROLE_ADMIN")
    }

    test("getPassword는 null 반환") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when & then
        userDetails.getPassword() shouldBe null
    }

    test("getUsername은 email을 반환") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "username@example.com",
            role = UserRole.USER
        )

        // when & then
        userDetails.getUsername() shouldBe "username@example.com"
    }

    test("isAccountNonExpired는 항상 true") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when & then
        userDetails.isAccountNonExpired() shouldBe true
    }

    test("isAccountNonLocked는 항상 true") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when & then
        userDetails.isAccountNonLocked() shouldBe true
    }

    test("isCredentialsNonExpired는 항상 true") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when & then
        userDetails.isCredentialsNonExpired() shouldBe true
    }

    test("isEnabled는 항상 true") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when & then
        userDetails.isEnabled() shouldBe true
    }

    test("UserDetails 인터페이스 모든 메서드 검증") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "full@example.com",
            role = UserRole.USER
        )

        // when & then
        userDetails.getUsername() shouldBe "full@example.com"
        userDetails.getPassword() shouldBe null
        userDetails.getAuthorities() shouldHaveSize 1
        userDetails.isAccountNonExpired() shouldBe true
        userDetails.isAccountNonLocked() shouldBe true
        userDetails.isCredentialsNonExpired() shouldBe true
        userDetails.isEnabled() shouldBe true
    }

    test("OAuth2User 인터페이스 메서드 검증") {
        // given
        val attributes = mapOf(
            "sub" to "oauth2-id",
            "email" to "oauth@example.com"
        )

        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "oauth@example.com",
            role = UserRole.USER,
            attributes = attributes
        )

        // when & then
        userDetails.getName() shouldBe "oauth@example.com"
        userDetails.getAttributes() shouldBe attributes
    }

    test("다양한 userId로 생성 가능") {
        // given
        val userIds = listOf(1L, 100L, 999999L, Long.MAX_VALUE)

        userIds.forEach { userId ->
            // when
            val userDetails = CustomUserDetails(
                userId = userId,
                email = "user$userId@example.com",
                role = UserRole.USER
            )

            // then
            userDetails.userId shouldBe userId
        }
    }

    test("다양한 이메일 형식 지원") {
        // given
        val emails = listOf(
            "simple@example.com",
            "user.name@example.com",
            "user+tag@example.com",
            "user@sub.example.co.kr"
        )

        emails.forEach { email ->
            // when
            val userDetails = CustomUserDetails(
                userId = 1L,
                email = email,
                role = UserRole.USER
            )

            // then
            userDetails.email shouldBe email
            userDetails.getUsername() shouldBe email
            userDetails.getName() shouldBe email
        }
    }

    test("OAuth2 attributes - 다양한 타입의 값") {
        // given
        val attributes = mapOf(
            "string" to "value",
            "number" to 123,
            "boolean" to true,
            "list" to listOf("item1", "item2"),
            "map" to mapOf("nested" to "value")
        )

        // when
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER,
            attributes = attributes
        )

        // then
        userDetails.getAttributes()["string"] shouldBe "value"
        userDetails.getAttributes()["number"] shouldBe 123
        userDetails.getAttributes()["boolean"] shouldBe true
    }

    test("동일한 정보로 생성된 두 객체는 동등하지 않음 (data class가 아님)") {
        // given
        val userDetails1 = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        val userDetails2 = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when & then
        (userDetails1 === userDetails2) shouldBe false
    }
})
