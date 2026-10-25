package com.techinsights.api.auth

import com.techinsights.api.props.AuthProperties
import com.techinsights.domain.dto.user.AuthUserDto
import com.techinsights.domain.dto.user.RefreshTokenRotationResult
import com.techinsights.domain.enums.UserRole
import com.techinsights.domain.exception.user.UserNotFoundException
import com.techinsights.domain.service.user.RefreshTokenService
import com.techinsights.domain.service.user.UserService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.jsonwebtoken.Claims
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.Base64

class TokenServiceTest : FunSpec({
    val jwtPlugin = mockk<JwtPlugin>()
    val refreshTokenService = mockk<RefreshTokenService>()
    val userService = mockk<UserService>()
    val authProperties = AuthProperties(
        jwt = AuthProperties.Jwt(
            secretKey = Base64.getEncoder().encodeToString("this-is-a-very-secure-secret-key!!".toByteArray()),
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

    val userId = 1L
    val user = AuthUserDto(id = userId, email = "test@example.com", role = UserRole.USER)

    fun givenValidRefreshToken(token: String) {
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId
        every { jwtPlugin.validateRefreshToken(token) } returns claims
        every { userService.getAuthUserById(userId) } returns user
        every { jwtPlugin.generateAccessToken(any(), any(), any()) } returns "new-at"
        every { jwtPlugin.generateRefreshToken(userId) } returns "new-rt"
    }

    test("유효한 리프레시 토큰으로 갱신 시 제시된 해시의 행이 회전되고 새로운 토큰 쌍이 발급되어야 한다 (RTR)") {
        // given
        givenValidRefreshToken("old-rt")
        every {
            refreshTokenService.rotate(tokenHasher.hash("old-rt"), tokenHasher.hash("new-rt"), any())
        } returns RefreshTokenRotationResult.Rotated

        // when
        val response = tokenService.refresh("old-rt")

        // then
        response.accessToken shouldBe "new-at"
        response.refreshToken shouldBe "new-rt"
        verify(exactly = 0) { refreshTokenService.upsertToken(any(), any(), any(), any()) }
        verify(exactly = 0) { refreshTokenService.deleteAllByUserId(any()) }
    }

    test("유예 기간 내 직전 토큰으로 요청해도 회전에 성공하면 세션을 유지해야 한다") {
        givenValidRefreshToken("old-rt")
        every { refreshTokenService.rotate(any(), any(), any()) } returns RefreshTokenRotationResult.Rotated

        val response = tokenService.refresh("old-rt")

        response.refreshToken shouldBe "new-rt"
        verify(exactly = 0) { refreshTokenService.deleteAllByUserId(any()) }
    }

    test("유예 기간이 지난 직전 토큰이 재사용되면 모든 세션을 폐기하고 TokenTamperedException이 발생해야 한다") {
        givenValidRefreshToken("old-rt")
        every { refreshTokenService.rotate(any(), any(), any()) } returns RefreshTokenRotationResult.ReuseDetected
        every { refreshTokenService.deleteAllByUserId(userId) } just Runs

        shouldThrow<TokenTamperedException> { tokenService.refresh("old-rt") }

        verify(exactly = 1) { refreshTokenService.deleteAllByUserId(userId) }
    }

    test("저장된 적 없는 해시는 세션 폐기 없이 InvalidTokenException이 발생해야 한다") {
        givenValidRefreshToken("unknown-rt")
        every { refreshTokenService.rotate(any(), any(), any()) } returns RefreshTokenRotationResult.NotFound

        shouldThrow<InvalidTokenException> { tokenService.refresh("unknown-rt") }

        verify(exactly = 0) { refreshTokenService.deleteAllByUserId(any()) }
    }

    test("만료된 리프레시 토큰은 ExpiredTokenException이 발생해야 한다") {
        givenValidRefreshToken("old-rt")
        every { refreshTokenService.rotate(any(), any(), any()) } returns RefreshTokenRotationResult.Expired

        shouldThrow<ExpiredTokenException> { tokenService.refresh("old-rt") }

        verify(exactly = 0) { refreshTokenService.deleteAllByUserId(any()) }
    }

    test("액세스 토큰 등 유형이 다른 토큰은 해시 조회나 세션 폐기 없이 InvalidTokenException이 발생해야 한다") {
        every { jwtPlugin.validateRefreshToken("access-token") } throws InvalidTokenException("토큰 유형이 올바르지 않습니다.")

        shouldThrow<InvalidTokenException> { tokenService.refresh("access-token") }

        verify(exactly = 0) { refreshTokenService.rotate(any(), any(), any()) }
        verify(exactly = 0) { refreshTokenService.deleteAllByUserId(any()) }
    }

    test("사용자를 찾을 수 없으면 InvalidTokenException이 발생해야 한다") {
        val claims = mockk<Claims>()
        every { claims.get("userId", Long::class.javaObjectType) } returns userId
        every { jwtPlugin.validateRefreshToken("old-rt") } returns claims
        every { userService.getAuthUserById(userId) } throws UserNotFoundException(userId)

        shouldThrow<InvalidTokenException> { tokenService.refresh("old-rt") }

        verify(exactly = 0) { refreshTokenService.rotate(any(), any(), any()) }
    }

    test("재사용 감지로 인한 세션 폐기와 만료 토큰 삭제는 예외가 발생해도 롤백되지 않아야 한다") {
        val noRollbackFor = TokenService::class.java
            .getMethod("refresh", String::class.java)
            .getAnnotation(Transactional::class.java)
            .noRollbackFor

        noRollbackFor.toSet() shouldBe setOf(TokenTamperedException::class, ExpiredTokenException::class)
    }
})
