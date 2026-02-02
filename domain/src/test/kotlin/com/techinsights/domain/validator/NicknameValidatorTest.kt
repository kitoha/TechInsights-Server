package com.techinsights.domain.validator

import com.techinsights.domain.exception.user.InvalidNicknameException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class NicknameValidatorTest : FunSpec({
    val validator = NicknameValidator()
    
    context("유효한 닉네임 검증") {
        test("한글만 사용한 닉네임") {
            shouldNotThrowAny {
                validator.validate("테스트")
            }
        }
        
        test("영문만 사용한 닉네임") {
            shouldNotThrowAny {
                validator.validate("TestUser")
            }
        }
        
        test("한글과 영문 혼합") {
            shouldNotThrowAny {
                validator.validate("테스트User")
            }
        }
        
        test("한글, 영문, 숫자 혼합") {
            shouldNotThrowAny {
                validator.validate("테스트User123")
            }
        }
        
        test("언더스코어 포함") {
            shouldNotThrowAny {
                validator.validate("test_user")
            }
        }
        
        test("하이픈 포함") {
            shouldNotThrowAny {
                validator.validate("test-user")
            }
        }
        
        test("최소 길이 (2자)") {
            shouldNotThrowAny {
                validator.validate("테스")
            }
        }
        
        test("최대 길이 (20자)") {
            shouldNotThrowAny {
                validator.validate("가".repeat(20))
            }
        }
        
        test("isValid는 유효한 닉네임에 true 반환") {
            validator.isValid("유효한닉네임") shouldBe true
        }
    }
    
    context("길이 검증") {
        test("빈 문자열 - 예외 발생") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("")
            }
            exception.validationReason shouldContain "필수"
        }
        
        test("1자 닉네임 - 너무 짧음") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("가")
            }
            exception.validationReason shouldContain "최소 2자"
        }
        
        test("21자 닉네임 - 너무 김") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("가".repeat(21))
            }
            exception.validationReason shouldContain "최대 20자"
        }
        
        test("공백만 있는 경우 trim 후 빈 문자열") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("   ")
            }
            exception.validationReason shouldContain "필수"
        }
    }
    
    context("문자 패턴 검증") {
        test("특수문자 포함 - 느낌표") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("테스트!")
            }
            exception.validationReason shouldContain "한글, 영문, 숫자, 언더스코어(_), 하이픈(-)"
        }
        
        test("특수문자 포함 - 공백") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("테스트 유저")
            }
            exception.validationReason shouldContain "한글, 영문, 숫자"
        }
        
        test("특수문자 포함 - 앳 사인") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("test@user")
            }
            exception.validationReason shouldContain "한글, 영문, 숫자"
        }
        
        test("이모지 포함") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("테스트😀")
            }
            exception.validationReason shouldContain "한글, 영문, 숫자"
        }
    }
    
    context("예약어 검증") {
        test("admin 포함") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("admin")
            }
            exception.validationReason shouldContain "예약된 단어"
        }
        
        test("administrator 포함") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("administrator")
            }
            exception.validationReason shouldContain "예약된 단어"
        }
        
        test("관리자 포함") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("관리자")
            }
            exception.validationReason shouldContain "예약된 단어"
        }
        
        test("system 포함") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("system_user")
            }
            exception.validationReason shouldContain "예약된 단어"
        }
        
        test("대소문자 구분 없이 예약어 검증") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("ADMIN")
            }
            exception.validationReason shouldContain "예약된 단어"
        }
        
        test("예약어가 중간에 포함된 경우") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("super_admin_user")
            }
            exception.validationReason shouldContain "예약된 단어"
        }
    }
    
    context("연속 특수문자 검증") {
        test("언더스코어 2개 연속") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("test__user")
            }
            exception.validationReason shouldContain "특수문자는 연속으로"
        }
        
        test("하이픈 2개 연속") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("test--user")
            }
            exception.validationReason shouldContain "특수문자는 연속으로"
        }
        
        test("언더스코어 3개 연속") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("test___user")
            }
            exception.validationReason shouldContain "특수문자는 연속으로"
        }
        
        test("단일 언더스코어는 허용") {
            shouldNotThrowAny {
                validator.validate("test_user")
            }
        }
        
        test("단일 하이픈은 허용") {
            shouldNotThrowAny {
                validator.validate("test-user")
            }
        }
    }
    
    context("숫자로만 구성 방지") {
        test("숫자로만 구성된 닉네임") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("12345")
            }
            exception.validationReason shouldContain "숫자로만 구성"
        }
        
        test("숫자와 문자 혼합은 허용") {
            shouldNotThrowAny {
                validator.validate("user123")
            }
        }
        
        test("숫자와 한글 혼합은 허용") {
            shouldNotThrowAny {
                validator.validate("테스트123")
            }
        }
    }
    
    context("trim 처리") {
        test("앞뒤 공백은 제거됨") {
            shouldNotThrowAny {
                validator.validate("  테스트  ")
            }
        }
        
        test("앞뒤 공백 제거 후 길이 검증") {
            val exception = shouldThrow<InvalidNicknameException> {
                validator.validate("  가  ")
            }
            exception.validationReason shouldContain "최소 2자"
        }
    }
    
    context("isValid 메서드") {
        test("유효하지 않은 닉네임에 false 반환") {
            validator.isValid("admin") shouldBe false
            validator.isValid("가") shouldBe false
            validator.isValid("test@user") shouldBe false
            validator.isValid("12345") shouldBe false
        }
        
        test("유효한 닉네임에 true 반환") {
            validator.isValid("유효한닉네임") shouldBe true
            validator.isValid("ValidUser") shouldBe true
            validator.isValid("user_123") shouldBe true
        }
    }
    
    context("엣지 케이스") {
        test("null 문자열은 NPE 발생 (의도된 동작)") {
            shouldThrow<NullPointerException> {
                validator.validate(null as String)
            }
        }
        
        test("유니코드 한글 조합") {
            shouldNotThrowAny {
                validator.validate("홍길동")
            }
        }
        
        test("영문 대소문자 혼합") {
            shouldNotThrowAny {
                validator.validate("TestUser")
            }
        }
    }
})
