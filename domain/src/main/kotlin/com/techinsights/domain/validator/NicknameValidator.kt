package com.techinsights.domain.validator

import com.techinsights.domain.exception.user.InvalidNicknameException
import org.springframework.stereotype.Component

@Component
class NicknameValidator {
    
    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 20
        private val ALLOWED_PATTERN = Regex("^[가-힣a-zA-Z0-9_-]+$")
        private val RESERVED_WORDS = setOf(
            "admin", "administrator", "root", "system", "null", "undefined",
            "관리자", "운영자", "시스템", "익명", "anonymous", "guest", "손님",
            "techinsights", "official", "공식"
        )
        private val CONSECUTIVE_SPECIAL_CHARS = Regex("[_-]{2,}")
        private val ONLY_NUMBERS = Regex("^[0-9]+$")
    }

    fun validate(nickname: String) {
        val trimmed = nickname.trim()

        // 1. 길이 검증
        validateLength(trimmed)

        // 2. 문자 패턴 검증
        validatePattern(trimmed)

        // 3. 예약어 검증
        validateReservedWords(trimmed)

        // 4. 연속 특수문자 검증
        validateConsecutiveSpecialChars(trimmed)
        
        // 5. 숫자로만 구성 방지
        validateNotOnlyNumbers(trimmed)
    }

    fun isValid(nickname: String): Boolean {
        return try {
            validate(nickname)
            true
        } catch (e: InvalidNicknameException) {
            false
        }
    }
    
    private fun validateLength(nickname: String) {
        when {
            nickname.isEmpty() -> 
                throw InvalidNicknameException(nickname, "닉네임은 필수입니다.")
            
            nickname.length < MIN_LENGTH -> 
                throw InvalidNicknameException(
                    nickname, 
                    "닉네임은 최소 ${MIN_LENGTH}자 이상이어야 합니다."
                )
            
            nickname.length > MAX_LENGTH -> 
                throw InvalidNicknameException(
                    nickname, 
                    "닉네임은 최대 ${MAX_LENGTH}자까지 가능합니다."
                )
        }
    }
    
    private fun validatePattern(nickname: String) {
        if (!ALLOWED_PATTERN.matches(nickname)) {
            throw InvalidNicknameException(
                nickname,
                "닉네임은 한글, 영문, 숫자, 언더스코어(_), 하이픈(-)만 사용할 수 있습니다."
            )
        }
    }
    
    private fun validateReservedWords(nickname: String) {
        val lowercaseNickname = nickname.lowercase()
        if (RESERVED_WORDS.any { lowercaseNickname.contains(it) }) {
            throw InvalidNicknameException(
                nickname,
                "예약된 단어는 사용할 수 없습니다."
            )
        }
    }
    
    private fun validateConsecutiveSpecialChars(nickname: String) {
        if (CONSECUTIVE_SPECIAL_CHARS.containsMatchIn(nickname)) {
            throw InvalidNicknameException(
                nickname,
                "특수문자는 연속으로 사용할 수 없습니다."
            )
        }
    }
    
    private fun validateNotOnlyNumbers(nickname: String) {
        if (ONLY_NUMBERS.matches(nickname)) {
            throw InvalidNicknameException(
                nickname,
                "숫자로만 구성된 닉네임은 사용할 수 없습니다."
            )
        }
    }
}
