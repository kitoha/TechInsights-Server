package com.techinsights.domain.exception

class UnauthorizedException(message: String = "인증이 필요합니다.") : CommonException(message)
