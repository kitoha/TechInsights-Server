package com.techinsights.api.service.auth

import com.techinsights.api.exception.auth.ExpiredTokenException
import com.techinsights.api.exception.auth.TokenTamperedException
import com.techinsights.api.util.auth.JwtPlugin
import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.repository.user.RefreshTokenRepository
import com.techinsights.domain.repository.user.UserRepository
import io.jsonwebtoken.Claims
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.time.Instant
import java.util.*

class TokenServiceTest : FunSpec({
    val jwtPlugin = mockk<JwtPlugin>()
    val refreshTokenRepository = mockk<RefreshTokenRepository>()
    val userRepository = mockk<UserRepository>()
    val tokenService = TokenService(jwtPlugin, refreshTokenRepository, userRepository)

    beforeTest {
        clearMocks(jwtPlugin, refreshTokenRepository, userRepository)
    }

    test("토큰 처음 발급 시 신규 리프레시 토큰이 DB에 저장되어야 한다") {
        // given
        val userId = 1L
        val email = "test@example.com"
        val role = UserRole.USER
        val deviceId = "device-1"

        every { refreshTokenRepository.findByUserAndDevice(userId, deviceId) } returns Optional.empty()
        every { jwtPlugin.generateAccessToken(userId, email, role) } returns "at-123"
        every { jwtPlugin.generateRefreshToken(userId) } returns "rt-123"
        every { refreshTokenRepository.save(any()) } returns mockk()

        // when
        val response = tokenService.issueTokens(userId, email, role, deviceId)

        // then
        response.accessToken shouldBe "at-123"
        response.refreshToken shouldBe "rt-123"
        verify(exactly = 1) { refreshTokenRepository.save(match { it.userId == userId && it.tokenHash == "rt-123" }) }
    }

    test("리프레시 토큰 갱신 시 RTR이 적용되어야 한다") {
        // given
        val oldRt = "old-rt"
        val userId = 1L
        val deviceId = "device-1"
        val existingToken = RefreshToken(1L, userId, oldRt, deviceId, Instant.now().plusSeconds(3600))
        val user = User(id = userId, email = "test@example.com", name = "Tester", googleSub = "sub", role = UserRole.USER)
        
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId

        every { jwtPlugin.validateToken(oldRt) } returns claims
        every { refreshTokenRepository.findByHash(oldRt) } returns Optional.of(existingToken)
        every { userRepository.findById(userId) } returns Optional.of(user)
        
        // Mock issueTokens dependencies
        every { refreshTokenRepository.findByUserAndDevice(userId, deviceId) } returns Optional.of(existingToken)
        every { jwtPlugin.generateAccessToken(any(), any(), any()) } returns "new-at"
        every { jwtPlugin.generateRefreshToken(userId) } returns "new-rt"
        every { refreshTokenRepository.save(any()) } returns mockk()

        // when
        val response = tokenService.refresh(oldRt, deviceId)

        // then
        response.accessToken shouldBe "new-at"
        response.refreshToken shouldBe "new-rt"
        existingToken.tokenHash shouldBe "new-rt"
    }

    test("DB에 없는 유효한 토큰으로 갱신 시도 시 이상 징후로 판단하고 모든 세션을 무효화해야 한다") {
        // given
        val stolenRt = "stolen-rt"
        val userId = 1L
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId

        every { jwtPlugin.validateToken(stolenRt) } returns claims
        every { refreshTokenRepository.findByHash(stolenRt) } returns Optional.empty()
        every { refreshTokenRepository.deleteAllByUserId(userId) } just Runs

        // when & then
        shouldThrow<TokenTamperedException> {
            tokenService.refresh(stolenRt, "attacker-device")
        }
        verify(exactly = 1) { refreshTokenRepository.deleteAllByUserId(userId) }
    }

    test("만료된 리프레시 토큰으로 갱신 시도 시 예외가 발생해야 한다") {
        // given
        val expiredRt = "expired-rt"
        val userId = 1L
        val expiredToken = RefreshToken(1L, userId, expiredRt, "dev", Instant.now().minusSeconds(10))
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId

        every { jwtPlugin.validateToken(expiredRt) } returns claims
        every { refreshTokenRepository.findByHash(expiredRt) } returns Optional.of(expiredToken)
        every { refreshTokenRepository.delete(expiredToken) } just Runs

        // when & then
        shouldThrow<ExpiredTokenException> {
            tokenService.refresh(expiredRt, "dev")
        }
    }
})
