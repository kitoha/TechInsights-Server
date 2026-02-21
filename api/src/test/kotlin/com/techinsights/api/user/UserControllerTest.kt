package com.techinsights.api.user

import com.techinsights.api.auth.CustomUserDetails
import com.techinsights.domain.dto.user.UserProfileDto
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.exception.user.DuplicateNicknameException
import com.techinsights.domain.exception.user.InvalidNicknameException
import com.techinsights.domain.exception.user.UserNotFoundException
import com.techinsights.domain.service.user.UserService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpStatus


class UserControllerTest : FunSpec({
    val userService = mockk<UserService>()
    val controller = UserController(userService)

    val testUser = UserProfileDto(
        id = 1L,
        email = "test@example.com",
        name = "Test User",
        nickname = "testuser",
        role = UserRole.USER,
        profileImage = "https://example.com/profile.png"
    )

    beforeTest {
        clearMocks(userService)
    }

    context("GET /api/v1/users/me - 내 정보 조회") {

        test("성공 - 사용자 정보 반환") {
            // given
            val userDetails = CustomUserDetails(
                userId = 1L,
                email = "test@example.com",
                role = UserRole.USER
            )
            every { userService.getUserProfileById(1L) } returns testUser

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

            verify(exactly = 1) { userService.getUserProfileById(1L) }
        }

        test("실패 - 미인증 사용자 (401)") {
            // when
            val response = controller.getMe(null)

            // then
            response.statusCode shouldBe HttpStatus.UNAUTHORIZED
            response.body shouldBe null

            verify(exactly = 0) { userService.getUserProfileById(any()) }
        }

        test("실패 - 사용자 없음 (UserNotFoundException)") {
            // given
            val userDetails = CustomUserDetails(
                userId = 999L,
                email = "notfound@example.com",
                role = UserRole.USER
            )
            every { userService.getUserProfileById(999L) } throws UserNotFoundException(999L)

            // when & then
            val exception = runCatching {
                controller.getMe(userDetails)
            }.exceptionOrNull()

            (exception is UserNotFoundException) shouldBe true
            verify(exactly = 1) { userService.getUserProfileById(999L) }
        }
    }

    context("POST /api/v1/users/me/nickname - 닉네임 변경") {

        test("성공 - 닉네임 변경") {
            // given
            val userDetails = CustomUserDetails(
                userId = 1L,
                email = "test@example.com",
                role = UserRole.USER
            )
            val request = UpdateNicknameRequest(nickname = "새로운닉네임")
            val updatedUser = UserProfileDto(
                id = testUser.id,
                email = testUser.email,
                name = testUser.name,
                nickname = "새로운닉네임",
                role = testUser.role,
                profileImage = testUser.profileImage
            )
            every { userService.updateNicknameProfile(1L, "새로운닉네임") } returns updatedUser

            // when
            val response = controller.updateMyNickname(userDetails, request)

            // then
            response.statusCode shouldBe HttpStatus.OK
            response.body?.nickname shouldBe "새로운닉네임"
            response.body?.id shouldBe 1L

            verify(exactly = 1) { userService.updateNicknameProfile(1L, "새로운닉네임") }
        }

        test("실패 - 중복된 닉네임 (DuplicateNicknameException)") {
            // given
            val userDetails = CustomUserDetails(
                userId = 1L,
                email = "test@example.com",
                role = UserRole.USER
            )
            val request = UpdateNicknameRequest(nickname = "중복닉네임")
            every { userService.updateNicknameProfile(1L, "중복닉네임") } throws DuplicateNicknameException("중복닉네임")

            // when & then
            val exception = runCatching {
                controller.updateMyNickname(userDetails, request)
            }.exceptionOrNull()

            (exception is DuplicateNicknameException) shouldBe true
            (exception as? DuplicateNicknameException)?.duplicatedNickname shouldBe "중복닉네임"

            verify(exactly = 1) { userService.updateNicknameProfile(1L, "중복닉네임") }
        }

        test("실패 - 유효하지 않은 닉네임 (InvalidNicknameException)") {
            // given
            val userDetails = CustomUserDetails(
                userId = 1L,
                email = "test@example.com",
                role = UserRole.USER
            )
            val request = UpdateNicknameRequest(nickname = "admin")
            every { userService.updateNicknameProfile(1L, "admin") } throws InvalidNicknameException("admin", "예약된 단어는 사용할 수 없습니다.")

            // when & then
            val exception = runCatching {
                controller.updateMyNickname(userDetails, request)
            }.exceptionOrNull()

            (exception is InvalidNicknameException) shouldBe true
            (exception as? InvalidNicknameException)?.invalidNickname shouldBe "admin"

            verify(exactly = 1) { userService.updateNicknameProfile(1L, "admin") }
        }

        test("실패 - 사용자 없음 (UserNotFoundException)") {
            // given
            val userDetails = CustomUserDetails(
                userId = 999L,
                email = "notfound@example.com",
                role = UserRole.USER
            )
            val request = UpdateNicknameRequest(nickname = "새닉네임")
            every { userService.updateNicknameProfile(999L, "새닉네임") } throws UserNotFoundException(999L)

            // when & then
            val exception = runCatching {
                controller.updateMyNickname(userDetails, request)
            }.exceptionOrNull()

            (exception is UserNotFoundException) shouldBe true

            verify(exactly = 1) { userService.updateNicknameProfile(999L, "새닉네임") }
        }

        test("성공 - 기존 닉네임과 동일 (업데이트 스킵)") {
            // given
            val userDetails = CustomUserDetails(
                userId = 1L,
                email = "test@example.com",
                role = UserRole.USER
            )
            val request = UpdateNicknameRequest(nickname = "testuser")
            every { userService.updateNicknameProfile(1L, "testuser") } returns testUser

            // when
            val response = controller.updateMyNickname(userDetails, request)

            // then
            response.statusCode shouldBe HttpStatus.OK
            response.body?.nickname shouldBe "testuser"

            verify(exactly = 1) { userService.updateNicknameProfile(1L, "testuser") }
        }
    }
})
