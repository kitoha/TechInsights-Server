package com.techinsights.domain.service.user

import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.ProviderType
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.exception.user.DuplicateNicknameException
import com.techinsights.domain.exception.user.InvalidNicknameException
import com.techinsights.domain.exception.user.UserNotFoundException
import com.techinsights.domain.repository.user.UserRepository
import com.techinsights.domain.validator.NicknameValidator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.DataIntegrityViolationException
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
        test("기존 Google 사용자면 email/name/profileImage를 갱신한다") {
            val existing = createTestUser().apply {
                email = "old@example.com"
                name = "Old Name"
                profileImage = "old.png"
            }

            every {
                userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123")
            } returns Optional.of(existing)
            every { userRepository.save(any()) } answers { it.invocation.args[0] as User }

            val result = userService.upsertGoogleOAuthUser(
                providerId = "google-123",
                email = "new@example.com",
                name = "New Name",
                profileImage = "new.png"
            )

            result.id shouldBe existing.id
            result.email shouldBe "new@example.com"
            existing.email shouldBe "new@example.com"
            existing.name shouldBe "New Name"
            existing.profileImage shouldBe "new.png"
            existing.lastLoginAt shouldNotBe null

            verify(exactly = 1) { userRepository.save(existing) }
        }

        test("동시성으로 save 충돌 시 재조회 후 복구 저장한다") {
            val existingAfterRace = createTestUser().apply {
                email = "before@example.com"
            }

            every {
                userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123")
            } returnsMany listOf(Optional.empty(), Optional.of(existingAfterRace))
            var saveInvocationCount = 0
            every { userRepository.save(any()) } answers {
                saveInvocationCount += 1
                if (saveInvocationCount == 1) {
                    throw DataIntegrityViolationException("duplicate")
                }
                it.invocation.args[0] as User
            }

            val result = userService.upsertGoogleOAuthUser(
                providerId = "google-123",
                email = "new@example.com",
                name = "New Name",
                profileImage = "new.png"
            )

            result.id shouldBe existingAfterRace.id
            result.email shouldBe "new@example.com"
            existingAfterRace.email shouldBe "new@example.com"

            verify(exactly = 2) { userRepository.save(any()) }
        }

        test("동시성 충돌 후 재조회에도 사용자가 없으면 예외를 재던진다") {
            every {
                userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, "google-123")
            } returnsMany listOf(Optional.empty(), Optional.empty())
            every { userRepository.save(any()) } throws DataIntegrityViolationException("duplicate")

            shouldThrow<DataIntegrityViolationException> {
                userService.upsertGoogleOAuthUser(
                    providerId = "google-123",
                    email = "new@example.com",
                    name = "New Name",
                    profileImage = "new.png"
                )
            }
        }
    }
})
