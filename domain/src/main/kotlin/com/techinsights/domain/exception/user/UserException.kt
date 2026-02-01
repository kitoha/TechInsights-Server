package com.techinsights.domain.exception.user

import com.techinsights.domain.enums.exception.UserErrorCode
import com.techinsights.domain.exception.CommonException

sealed class UserException(
    message: String,
    val errorCode: UserErrorCode
) : CommonException(message) {
    
    override fun toString(): String {
        return "${this::class.simpleName}(errorCode=$errorCode, message=$message)"
    }
}

class UserNotFoundException(
    userId: Long? = null,
    message: String = userId?.let { "사용자를 찾을 수 없습니다. (ID: $it)" } 
        ?: "사용자를 찾을 수 없습니다."
) : UserException(message, UserErrorCode.USER_NOT_FOUND)

class DuplicateNicknameException(
    nickname: String,
    message: String = "이미 사용 중인 닉네임입니다. (닉네임: $nickname)"
) : UserException(message, UserErrorCode.DUPLICATE_NICKNAME) {
    val duplicatedNickname: String = nickname
}

class InvalidNicknameException(
    nickname: String,
    reason: String,
    message: String = "유효하지 않은 닉네임입니다. (닉네임: $nickname, 사유: $reason)"
) : UserException(message, UserErrorCode.INVALID_NICKNAME) {
    val invalidNickname: String = nickname
    val validationReason: String = reason
}