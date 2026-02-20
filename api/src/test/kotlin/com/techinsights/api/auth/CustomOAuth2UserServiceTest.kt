package com.techinsights.api.auth

import com.techinsights.domain.dto.user.AuthUserDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import org.springframework.security.oauth2.core.user.DefaultOAuth2User

import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.service.user.UserService

class CustomOAuth2UserServiceTest : FunSpec({
    val userService = mockk<UserService>()
    val customOAuth2UserService = CustomOAuth2UserService(userService)

    beforeTest {
        clearMocks(userService)
    }

    test("Google 로그인이 성공하면 신규 사용자를 저장하고 CustomUserDetails를 반환한다") {
        // given
        val attributes = mapOf(
            "sub" to "google-123",
            "email" to "test@example.com",
            "name" to "Tester",
            "picture" to "image.png"
        )
        val oAuth2User = DefaultOAuth2User(emptyList(), attributes, "sub")

        every {
            userService.upsertGoogleOAuthUser("google-123", "test@example.com", "Tester", "image.png")
        } returns AuthUserDto(
            id = 100L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when
        val result = customOAuth2UserService.processOAuth2User(oAuth2User)

        // then
        result.shouldBeInstanceOf<CustomUserDetails>()
        val userDetails = result as CustomUserDetails
        userDetails.userId shouldNotBe null
        userDetails.username shouldBe "test@example.com"
        
        verify(exactly = 1) {
            userService.upsertGoogleOAuthUser("google-123", "test@example.com", "Tester", "image.png")
        }
    }

    test("이미 존재하는 사용자의 경우 정보를 업데이트한다") {
        // given
        val attributes = mapOf(
            "sub" to "google-123",
            "email" to "test@example.com",
            "name" to "New Name",
            "picture" to "new-image.png"
        )
        val oAuth2User = DefaultOAuth2User(emptyList(), attributes, "sub")

        every {
            userService.upsertGoogleOAuthUser("google-123", "test@example.com", "New Name", "new-image.png")
        } returns AuthUserDto(
            id = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        // when
        val result = customOAuth2UserService.processOAuth2User(oAuth2User)

        // then
        val userDetails = result as CustomUserDetails
        userDetails.userId shouldBe 1L
        
        verify(exactly = 1) {
            userService.upsertGoogleOAuthUser("google-123", "test@example.com", "New Name", "new-image.png")
        }
    }
})
