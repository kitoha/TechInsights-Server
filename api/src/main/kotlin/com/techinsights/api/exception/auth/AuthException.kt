package com.techinsights.api.exception.auth

sealed class AuthException(
    override val message: String,
    val errorCode: String
) : RuntimeException(message)

class ExpiredTokenException(message: String = "토큰이 만료되었습니다.") : 
    AuthException(message, "EXPIRED_TOKEN")

class InvalidTokenException(message: String = "유효하지 않은 토큰입니다.") : 
    AuthException(message, "INVALID_TOKEN")

class TokenTamperedException(message: String = "토큰이 변조되었습니다.") : 
    AuthException(message, "TOKEN_TAMPERED")

class UnauthorizedException(message: String = "인증이 필요한 서비스입니다.") : 
    AuthException(message, "UNAUTHORIZED")
