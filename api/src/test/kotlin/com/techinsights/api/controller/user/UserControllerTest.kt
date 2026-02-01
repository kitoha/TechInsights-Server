package com.techinsights.api.controller.user

import com.techinsights.api.util.auth.CustomUserDetails
import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.repository.user.UserRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpStatus
import java.util.*

class UserControllerTest : FunSpec({
    val userRepository = mockk<UserRepository>()
    val controller = UserController(userRepository)

    val testUser = User(
        id = 1L,
        email = "test@example.com",
        name = "Test User",
        nickname = "testuser",
        provider = ProviderType.GOOGLE,
        providerId = "google-123",
        role = UserRole.USER,
        profileImage = "https://example.com/profile.png"
    )

    beforeTest {
        clearMocks(userRepository)
    }

    test("인증된 사용자 정보 조회 - 성공") {
        // given
        val userDetails = CustomUserDetails(
            userId = 1L,
            email = "test@example.com",
            role = UserRole.USER
        )

        every { userRepository.findById(1L) } returns Optional.of(testUser)

        // when
        val response = controller.getMe(userDetails)

        // then
        response.statusCode shouldBe HttpStatus.OK
        response.body?.id shouldBe 1L
        response.body?.email shouldBe "test@example.com"
        response.body?.name shouldBe "Test User"
        response.body?.nickname shouldBe "testuser"
        response.body?.profileImage shouldBe "https://example.com/profile.png"
        response.body?.role shouldBe UserRole.USER

        verify(exactly = 1) { userRepository.findById(1L) }
    }

    test("인증된 사용자 정보 조회 - ADMIN 권한") {
        // given
        val adminUser = User(
            id = 2L,
            email = "admin@example.com",
            name = "Admin User",
            nickname = "admin",
            provider = ProviderType.GOOGLE,
            providerId = "google-456",
            role = UserRole.ADMIN,
            profileImage = null
        )

        val userDetails = CustomUserDetails(
            userId = 2L,
            email = "admin@example.com",
            role = UserRole.ADMIN
        )

        every { userRepository.findById(2L) } returns Optional.of(adminUser)

        // when
        val response = controller.getMe(userDetails)

        // then
        response.statusCode shouldBe HttpStatus.OK
        response.body?.id shouldBe 2L
        response.body?.role shouldBe UserRole.ADMIN
        response.body?.profileImage shouldBe null

        verify(exactly = 1) { userRepository.findById(2L) }
    }

    test("미인증 사용자 조회 - userDetails가 null인 경우 401 반환") {
        // when
        val response = controller.getMe(null)

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.body shouldBe null

        verify(exactly = 0) { userRepository.findById(any()) }
    }

    test("사용자를 찾을 수 없는 경우 RuntimeException 발생") {
        // given
        val userDetails = CustomUserDetails(
            userId = 999L,
            email = "notfound@example.com",
            role = UserRole.USER
        )

        every { userRepository.findById(999L) } returns Optional.empty()

        // when & then
        val exception = runCatching {
            controller.getMe(userDetails)
        }.exceptionOrNull()

        (exception is RuntimeException) shouldBe true
        (exception as? RuntimeException)?.message shouldBe "User not found"

        verify(exactly = 1) { userRepository.findById(999L) }
    }

    test("프로필 이미지가 없는 사용자도 정상 조회") {
        // given
        val userWithoutImage = User(
            id = 3L,
            email = "noimage@example.com",
            name = "No Image User",
            nickname = "noimage",
            provider = ProviderType.KAKAO,
            providerId = "kakao-789",
            role = UserRole.USER,
            profileImage = null
        )

        val userDetails = CustomUserDetails(
            userId = 3L,
            email = "noimage@example.com",
            role = UserRole.USER
        )

        every { userRepository.findById(3L) } returns Optional.of(userWithoutImage)

        // when
        val response = controller.getMe(userDetails)

        // then
        response.statusCode shouldBe HttpStatus.OK
        response.body?.profileImage shouldBe null

        verify(exactly = 1) { userRepository.findById(3L) }
    }

    test("다양한 Provider 타입의 사용자 조회") {
        // given
        val providers = listOf(
            Triple(ProviderType.GOOGLE, "google-id", 1L),
            Triple(ProviderType.KAKAO, "kakao-id", 2L),
            Triple(ProviderType.NAVER, "naver-id", 3L)
        )

        providers.forEachIndexed { index, (provider, providerId, userId) ->
            val user = User(
                id = userId,
                email = "user$index@example.com",
                name = "User $index",
                nickname = "user$index",
                provider = provider,
                providerId = providerId,
                role = UserRole.USER
            )

            val userDetails = CustomUserDetails(
                userId = userId,
                email = "user$index@example.com",
                role = UserRole.USER
            )

            every { userRepository.findById(userId) } returns Optional.of(user)

            // when
            val response = controller.getMe(userDetails)

            // then
            response.statusCode shouldBe HttpStatus.OK
            response.body?.id shouldBe userId
        }

        verify(exactly = providers.size) { userRepository.findById(any()) }
    }
})
