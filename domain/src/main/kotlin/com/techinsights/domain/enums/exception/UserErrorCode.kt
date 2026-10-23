package com.techinsights.domain.enums.exception

enum class UserErrorCode(val code: String) {
    USER_NOT_FOUND("USER_001"),
    DUPLICATE_NICKNAME("USER_002"),
    INVALID_NICKNAME("USER_003"),
    ACCOUNT_DEACTIVATED("USER_004");

    override fun toString(): String = code
}
