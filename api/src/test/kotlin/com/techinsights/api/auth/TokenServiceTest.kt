package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.dto.user.AuthUserDto
import com.techinsights.domain.dto.user.RefreshTokenDto
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.service.user.RefreshTokenService
import com.techinsights.domain.service.user.UserService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.jsonwebtoken.Claims
import java.time.Instant
import java.time.LocalDateTime
import java.time.Duration

class TokenServiceTest : FunSpec({
    val jwtPlugin = mockk<JwtPlugin>()
    val refreshTokenService = mockk<RefreshTokenService>()
    val userService = mockk<UserService>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = "this-is-a-very-secure-secret-key-for-testing-purposes-only",
            refreshTokenExpiration = Duration.ofDays(30)
        )
    )
    val tokenHasher = TokenHasher(authProperties)
    val tokenService = TokenService(jwtPlugin, refreshTokenService, userService, authProperties, tokenHasher)

    beforeTest {
        clearMocks(jwtPlugin, refreshTokenService, userService)
    }

    test("토큰 발급 시 액세스 토큰과 리프레시 토큰이 레포지토리에 저장되고 반환되어야 한다") {
        // given
        val userId = 1L
        val email = "test@example.com"
        val role = UserRole.USER
        val deviceId = "test-device"
        
        every { jwtPlugin.generateAccessToken(userId, email, role) } returns "access-token"
        every { jwtPlugin.generateRefreshToken(userId) } returns "refresh-token"
        every { refreshTokenService.upsertToken(userId, deviceId, any(), any()) } returns mockk()

        // when
        val response = tokenService.issueTokens(userId, email, role, deviceId)

        // then
        response.accessToken shouldBe "access-token"
        response.refreshToken shouldBe "refresh-token"
        verify { refreshTokenService.upsertToken(userId, deviceId, any(), any()) }
    }

    test("유효한 리프레시 토큰으로 갱신 시 새로운 토큰 쌍이 발급되어야 한다 (RTR)") {
        // given
        val oldRt = "old-rt"
        val userId = 1L
        val existingToken = RefreshTokenDto(
            userId = userId,
            tokenHash = tokenHasher.hash(oldRt),
            previousTokenHash = null,
            deviceId = "dev",
            expiryAt = Instant.now().plusSeconds(3600),
            updatedAt = LocalDateTime.now()
        )
        val user = AuthUserDto(id = userId, email = "test@example.com", role = UserRole.USER)
        
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId

        every { jwtPlugin.validateToken(oldRt) } returns claims
        every { refreshTokenService.findByHash(tokenHasher.hash(oldRt)) } returns existingToken
        every { userService.getAuthUserById(userId) } returns user
        
        every { jwtPlugin.generateAccessToken(any(), any(), any()) } returns "new-at"
        every { jwtPlugin.generateRefreshToken(userId) } returns "new-rt"
        every { refreshTokenService.upsertToken(userId, "dev", any(), any()) } returns mockk()

        // when
        val response = tokenService.refresh(oldRt, "dev")

        // then
        response.accessToken shouldBe "new-at"
        response.refreshToken shouldBe "new-rt"
        verify { refreshTokenService.upsertToken(userId, "dev", any(), any()) }
    }

    test("짧은 유예 기간(Leeway) 내에 이전 토큰으로 갱신 시도 시 race condition으로 판단하고 세션을 유지해야 한다") {
        // given
        val oldRt = "old-rt"
        val userId = 1L
        val deviceId = "device-1"
        val existingToken = RefreshTokenDto(
            userId = userId,
            tokenHash = tokenHasher.hash("current-rt"),
            previousTokenHash = tokenHasher.hash(oldRt),
            deviceId = deviceId,
            expiryAt = Instant.now().plusSeconds(3600),
            updatedAt = LocalDateTime.now()
        )
        val user = AuthUserDto(id = userId, email = "test@example.com", role = UserRole.USER)
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId

        every { jwtPlugin.validateToken(oldRt) } returns claims
        every { refreshTokenService.findByHash(tokenHasher.hash(oldRt)) } returns null
        every { refreshTokenService.findByPreviousHash(tokenHasher.hash(oldRt)) } returns existingToken

        every { userService.getAuthUserById(userId) } returns user
        every { jwtPlugin.generateAccessToken(any(), any(), any()) } returns "new-at"
        every { jwtPlugin.generateRefreshToken(userId) } returns "new-rt"
        every { refreshTokenService.upsertToken(any(), any(), any(), any()) } returns mockk()

        // when
        val response = tokenService.refresh(oldRt, deviceId)

        // then
        response.accessToken shouldBe "new-at"
        response.refreshToken shouldBe "new-rt"
        verify(exactly = 0) { refreshTokenService.deleteAllByUserId(any()) }
    }
})
