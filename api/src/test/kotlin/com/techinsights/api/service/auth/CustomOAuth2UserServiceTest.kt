package com.techinsights.api.service.auth

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.repository.user.UserRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import java.util.*

import com.techinsights.api.util.auth.CustomUserDetails
import com.techinsights.domain.enums.UserRole

class CustomOAuth2UserServiceTest : FunSpec({
    val userRepository = mockk<UserRepository>()
    val customOAuth2UserService = CustomOAuth2UserService(userRepository)

    beforeTest {
        clearMocks(userRepository)
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

        every { userRepository.findByGoogleSub("google-123") } returns Optional.empty()
        every { userRepository.save(any()) } answers { it.invocation.args[0] as User }

        // when
        val result = customOAuth2UserService.processOAuth2User(oAuth2User)

        // then
        result.shouldBeInstanceOf<CustomUserDetails>()
        val userDetails = result as CustomUserDetails
        userDetails.userId shouldNotBe null
        userDetails.username shouldBe "test@example.com"
        
        verify(exactly = 1) { userRepository.save(match { it.googleSub == "google-123" }) }
    }

    test("이미 존재하는 사용자의 경우 정보를 업데이트한다") {
        // given
        val existingUser = User(
            id = 1L,
            email = "test@example.com",
            name = "Old Name",
            googleSub = "google-123",
            role = UserRole.USER
        )
        val attributes = mapOf(
            "sub" to "google-123",
            "email" to "test@example.com",
            "name" to "New Name",
            "picture" to "new-image.png"
        )
        val oAuth2User = DefaultOAuth2User(emptyList(), attributes, "sub")

        every { userRepository.findByGoogleSub("google-123") } returns Optional.of(existingUser)
        every { userRepository.save(any()) } answers { it.invocation.args[0] as User }

        // when
        val result = customOAuth2UserService.processOAuth2User(oAuth2User)

        // then
        val userDetails = result as CustomUserDetails
        userDetails.userId shouldBe 1L
        
        verify(exactly = 1) { 
            userRepository.save(match { 
                it.name == "New Name" && it.profileImage == "new-image.png" 
            }) 
        }
    }
})
