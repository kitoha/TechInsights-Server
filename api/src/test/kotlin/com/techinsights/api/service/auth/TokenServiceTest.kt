package com.techinsights.api.service.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.api.util.auth.JwtPlugin
import com.techinsights.api.util.auth.TokenHasher
import com.techinsights.domain.entity.user.RefreshToken
import com.techinsights.domain.entity.user.User
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.repository.user.RefreshTokenRepository
import com.techinsights.domain.repository.user.UserRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.jsonwebtoken.Claims
import java.time.Instant
import java.util.Optional
import java.time.Duration

import com.techinsights.domain.enums.ProviderType

class TokenServiceTest : FunSpec({
    val jwtPlugin = mockk<JwtPlugin>()
    val refreshTokenRepository = mockk<RefreshTokenRepository>()
    val userRepository = mockk<UserRepository>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = "this-is-a-very-secure-secret-key-for-testing-purposes-only",
            refreshTokenExpiration = Duration.ofDays(30)
        )
    )
    val tokenHasher = TokenHasher(authProperties)
    val tokenService = TokenService(jwtPlugin, refreshTokenRepository, userRepository, authProperties, tokenHasher)

    beforeTest {
        clearMocks(jwtPlugin, refreshTokenRepository, userRepository)
    }

    test("토큰 발급 시 액세스 토큰과 리프레시 토큰이 레포지토리에 저장되고 반환되어야 한다") {
        // given
        val userId = 1L
        val email = "test@example.com"
        val role = UserRole.USER
        val deviceId = "test-device"
        
        every { jwtPlugin.generateAccessToken(userId, email, role) } returns "access-token"
        every { jwtPlugin.generateRefreshToken(userId) } returns "refresh-token"
        every { refreshTokenRepository.findByUserAndDevice(userId, deviceId) } returns Optional.empty()
        every { refreshTokenRepository.save(any()) } returns mockk()

        // when
        val response = tokenService.issueTokens(userId, email, role, deviceId)

        // then
        response.accessToken shouldBe "access-token"
        response.refreshToken shouldBe "refresh-token"
        verify { refreshTokenRepository.save(any()) }
    }

    test("유효한 리프레시 토큰으로 갱신 시 새로운 토큰 쌍이 발급되어야 한다 (RTR)") {
        // given
        val oldRt = "old-rt"
        val userId = 1L
        val existingToken = RefreshToken(id = 1L, userId = userId, tokenHash = tokenHasher.hash(oldRt), deviceId = "dev", expiryAt = Instant.now().plusSeconds(3600))
        val user = User(id = userId, email = "test@example.com", name = "Tester", nickname = "tester", provider = ProviderType.GOOGLE, providerId = "sub", role = UserRole.USER)
        
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId

        every { jwtPlugin.validateToken(oldRt) } returns claims
        every { refreshTokenRepository.findByHash(tokenHasher.hash(oldRt)) } returns Optional.of(existingToken)
        every { userRepository.findById(userId) } returns Optional.of(user)
        
        every { jwtPlugin.generateAccessToken(any(), any(), any()) } returns "new-at"
        every { jwtPlugin.generateRefreshToken(userId) } returns "new-rt"
        every { refreshTokenRepository.findByUserAndDevice(userId, "dev") } returns Optional.of(existingToken)
        every { refreshTokenRepository.save(any()) } returns mockk()

        // when
        val response = tokenService.refresh(oldRt, "dev")

        // then
        response.accessToken shouldBe "new-at"
        response.refreshToken shouldBe "new-rt"
        verify { refreshTokenRepository.save(any()) }
    }

    test("짧은 유예 기간(Leeway) 내에 이전 토큰으로 갱신 시도 시 race condition으로 판단하고 세션을 유지해야 한다") {
        // given
        val oldRt = "old-rt"
        val userId = 1L
        val deviceId = "device-1"
        val existingToken = RefreshToken(id = 1L, userId = userId, tokenHash = tokenHasher.hash("current-rt"), deviceId = deviceId, expiryAt = Instant.now().plusSeconds(3600)).apply {
            previousTokenHash = tokenHasher.hash(oldRt)
            updatedAt = java.time.LocalDateTime.now()
        }
        val user = User(id = userId, email = "test@example.com", name = "Tester", nickname = "tester", provider = ProviderType.GOOGLE, providerId = "sub", role = UserRole.USER)
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId

        every { jwtPlugin.validateToken(oldRt) } returns claims
        every { refreshTokenRepository.findByHash(tokenHasher.hash(oldRt)) } returns Optional.empty()
        every { refreshTokenRepository.findByPreviousHash(tokenHasher.hash(oldRt)) } returns Optional.of(existingToken)

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { refreshTokenRepository.findByUserAndDevice(any(), any()) } returns Optional.of(existingToken)
        every { jwtPlugin.generateAccessToken(any(), any(), any()) } returns "new-at"
        every { jwtPlugin.generateRefreshToken(userId) } returns "new-rt"
        every { refreshTokenRepository.save(any()) } returns mockk()

        // when
        val response = tokenService.refresh(oldRt, deviceId)

        // then
        response.accessToken shouldBe "new-at"
        response.refreshToken shouldBe "new-rt"
        verify(exactly = 0) { refreshTokenRepository.deleteAllByUserId(any()) }
    }
})
