package com.techinsights.domain.service.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.enums.UserStatus
import com.techinsights.domain.exception.user.DuplicateNicknameException
import com.techinsights.domain.exception.user.InvalidNicknameException
import com.techinsights.domain.exception.user.UserNotFoundException
import com.techinsights.domain.repository.user.UserRepository
import com.techinsights.domain.validator.NicknameValidator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class UserServiceTest : FunSpec({
    val userRepository = mockk<UserRepository>()
    val nicknameValidator = mockk<NicknameValidator>()
    val userService = UserService(userRepository, nicknameValidator)

    fun createTestUser(nickname: String = "oldNickname") = User(
        id = 1L,
        email = "test@example.com",
        name = "Test User",
        nickname = nickname,
        provider = ProviderType.GOOGLE,
        providerId = "google-123",
        role = UserRole.USER
    )

    beforeTest {
        clearMocks(userRepository, nicknameValidator)
    }

    context("findAuthUserById") {
        test("성공 - 사용자가 존재하면 상태를 포함한 AuthUserDto를 반환한다") {
            every { userRepository.findById(1L) } returns Optional.of(createTestUser())

            val result = userService.findAuthUserById(1L)

            result?.id shouldBe 1L
            result?.status shouldBe UserStatus.ACTIVE
        }

        test("사용자가 없으면 예외 없이 null을 반환한다 (공유 트랜잭션을 rollback-only로 만들지 않기 위함)") {
            every { userRepository.findById(1L) } returns Optional.empty()

            userService.findAuthUserById(1L) shouldBe null
        }
    }

    context("getUserById") {
        test("성공 - 사용자가 존재하면 반환한다") {
            val user = createTestUser()
            every { userRepository.findById(1L) } returns Optional.of(user)

            val result = userService.getUserById(1L)

            result shouldBe user
            verify(exactly = 1) { userRepository.findById(1L) }
        }

        test("실패 - 사용자가 없으면 UserNotFoundException이 발생한다") {
            every { userRepository.findById(1L) } returns Optional.empty()

            shouldThrow<UserNotFoundException> {
                userService.getUserById(1L)
            }
        }
    }

    context("updateNickname") {
        test("성공 - 유효하고 중복되지 않은 닉네임으로 변경한다") {
            val user = createTestUser("oldNickname")
            val newNickname = "newNickname"
            
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { nicknameValidator.validate(newNickname) } returns Unit
            every { userRepository.existsByNickname(newNickname) } returns false
            every { userRepository.save(any()) } answers { it.invocation.args[0] as User }

            val result = userService.updateNickname(1L, newNickname)

            result.nickname shouldBe newNickname
            verify { nicknameValidator.validate(newNickname) }
            verify { userRepository.existsByNickname(newNickname) }
            verify { userRepository.save(any()) }
        }

        test("스킵 - 기존 닉네임과 동일하면 검증 없이 즉시 반환한다") {
            val currentNickname = "oldNickname"
            val user = createTestUser(currentNickname)
            
            every { userRepository.findById(1L) } returns Optional.of(user)

            val result = userService.updateNickname(1L, currentNickname)

            result.nickname shouldBe currentNickname
            verify(exactly = 0) { nicknameValidator.validate(any()) }
            verify(exactly = 0) { userRepository.existsByNickname(any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        test("실패 - 유효성 검증 실패 시 InvalidNicknameException이 발생한다") {
            val user = createTestUser()
            val invalidNickname = "a"
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { nicknameValidator.validate(invalidNickname) } throws InvalidNicknameException(invalidNickname, "너무 짧음")

            shouldThrow<InvalidNicknameException> {
                userService.updateNickname(1L, invalidNickname)
            }
            verify(exactly = 0) { userRepository.existsByNickname(any()) }
        }

        test("실패 - 중복된 닉네임이면 DuplicateNicknameException이 발생한다") {
            val user = createTestUser()
            val duplicateNickname = "alreadyTaken"
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { nicknameValidator.validate(duplicateNickname) } returns Unit
            every { userRepository.existsByNickname(duplicateNickname) } returns true

            shouldThrow<DuplicateNicknameException> {
                userService.updateNickname(1L, duplicateNickname)
            }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        test("실패 - 사용자가 존재하지 않으면 UserNotFoundException이 발생한다") {
            every { userRepository.findById(1L) } returns Optional.empty()

            shouldThrow<UserNotFoundException> {
                userService.updateNickname(1L, "any")
            }
        }
    }

    context("upsertGoogleOAuthUser") {
        test("원자적 upsert 후 저장된 사용자를 재조회해 반환한다") {
            val stored = createTestUser().apply {
                email = "new@example.com"
                name = "New Name"
                profileImage = "new.png"
                login()
            }

            every { userRepository.upsertByProvider(any(), any(), any(), any(), any(), any(), any()) } returns Unit
            every {
                userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123")
            } returns Optional.of(stored)

            val result = userService.upsertGoogleOAuthUser(
                providerId = "google-123",
                email = "new@example.com",
                name = "New Name",
                profileImage = "new.png"
            )

            result.id shouldBe stored.id
            result.email shouldBe "new@example.com"

            verify(exactly = 1) {
                userRepository.upsertByProvider(
                    id = any(),
                    email = "new@example.com",
                    name = "New Name",
                    nickname = match { it.startsWith("User_") },
                    provider = ProviderType.GOOGLE,
                    providerId = "google-123",
                    profileImage = "new.png"
                )
            }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        test("upsert 후에도 사용자를 찾을 수 없으면 예외를 던진다") {
            every { userRepository.upsertByProvider(any(), any(), any(), any(), any(), any(), any()) } returns Unit
            every {
                userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123")
            } returns Optional.empty()

            shouldThrow<IllegalStateException> {
                userService.upsertGoogleOAuthUser(
                    providerId = "google-123",
                    email = "new@example.com",
                    name = "New Name",
                    profileImage = null
                )
            }
        }
    }
})
