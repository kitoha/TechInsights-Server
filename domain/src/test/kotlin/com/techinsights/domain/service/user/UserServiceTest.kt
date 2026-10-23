package com.techinsights.domain.service.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.enums.UserStatus
import com.techinsights.domain.exception.user.AccountDeactivatedException
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
            every { userRepository.existsByNicknameIgnoreCaseAndIdNot(newNickname, 1L) } returns false
            every { userRepository.saveNickname(any()) } answers { it.invocation.args[0] as User }

            val result = userService.updateNickname(1L, newNickname)

            result.nickname shouldBe newNickname
            verify { nicknameValidator.validate(newNickname) }
            verify { userRepository.existsByNicknameIgnoreCaseAndIdNot(newNickname, 1L) }
            verify { userRepository.saveNickname(any()) }
        }

        test("성공 - 대소문자만 바꾸는 변경은 본인을 중복으로 취급하지 않는다") {
            val user = createTestUser("oldNickname")
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { nicknameValidator.validate("OldNickname") } returns Unit
            every { userRepository.existsByNicknameIgnoreCaseAndIdNot("OldNickname", 1L) } returns false
            every { userRepository.saveNickname(any()) } answers { it.invocation.args[0] as User }

            userService.updateNickname(1L, "OldNickname").nickname shouldBe "OldNickname"
        }

        test("스킵 - 기존 닉네임과 동일하면 검증 없이 즉시 반환한다") {
            val currentNickname = "oldNickname"
            val user = createTestUser(currentNickname)
            
            every { userRepository.findById(1L) } returns Optional.of(user)

            val result = userService.updateNickname(1L, currentNickname)

            result.nickname shouldBe currentNickname
            verify(exactly = 0) { nicknameValidator.validate(any()) }
            verify(exactly = 0) { userRepository.existsByNicknameIgnoreCaseAndIdNot(any(), any()) }
            verify(exactly = 0) { userRepository.saveNickname(any()) }
        }

        test("실패 - 유효성 검증 실패 시 InvalidNicknameException이 발생한다") {
            val user = createTestUser()
            val invalidNickname = "a"
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { nicknameValidator.validate(invalidNickname) } throws InvalidNicknameException(invalidNickname, "너무 짧음")

            shouldThrow<InvalidNicknameException> {
                userService.updateNickname(1L, invalidNickname)
            }
            verify(exactly = 0) { userRepository.existsByNicknameIgnoreCaseAndIdNot(any(), any()) }
        }

        test("실패 - 중복된 닉네임이면 DuplicateNicknameException이 발생한다") {
            val user = createTestUser()
            val duplicateNickname = "alreadyTaken"
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { nicknameValidator.validate(duplicateNickname) } returns Unit
            every { userRepository.existsByNicknameIgnoreCaseAndIdNot(duplicateNickname, 1L) } returns true

            shouldThrow<DuplicateNicknameException> {
                userService.updateNickname(1L, duplicateNickname)
            }
            verify(exactly = 0) { userRepository.saveNickname(any()) }
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

            every { userRepository.isDeactivatedAccount(ProviderType.GOOGLE, "google-123") } returns false
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
            every { userRepository.isDeactivatedAccount(ProviderType.GOOGLE, "google-123") } returns false
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

        test("삭제되었거나 비활성인 계정이면 아무것도 쓰지 않고 AccountDeactivatedException 을 던진다") {
            every { userRepository.isDeactivatedAccount(ProviderType.GOOGLE, "google-123") } returns true

            shouldThrow<AccountDeactivatedException> {
                userService.upsertGoogleOAuthUser("google-123", "new@example.com", "New Name", null)
            }

            verify(exactly = 0) { userRepository.upsertByProvider(any(), any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { userRepository.save(any()) }
        }

        test("확인과 upsert 사이에 비활성화되어 upsert 가 행을 바꾸지 못한 경우에도 거부한다") {
            val inactive = createTestUser().apply { status = UserStatus.INACTIVE }
            every { userRepository.isDeactivatedAccount(ProviderType.GOOGLE, "google-123") } returns false
            every { userRepository.upsertByProvider(any(), any(), any(), any(), any(), any(), any()) } returns Unit
            every {
                userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123")
            } returns Optional.of(inactive)

            shouldThrow<AccountDeactivatedException> {
                userService.upsertGoogleOAuthUser("google-123", "new@example.com", "New Name", null)
            }
        }

        test("확인과 upsert 사이에 soft-delete 되어 재조회가 비면 AccountDeactivatedException 을 던진다 (IllegalStateException 아님)") {
            every { userRepository.isDeactivatedAccount(ProviderType.GOOGLE, "google-123") } returnsMany listOf(false, true)
            every { userRepository.upsertByProvider(any(), any(), any(), any(), any(), any(), any()) } returns Unit
            every {
                userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123")
            } returns Optional.empty()

            shouldThrow<AccountDeactivatedException> {
                userService.upsertGoogleOAuthUser("google-123", "new@example.com", "New Name", null)
            }
        }
    }
})
