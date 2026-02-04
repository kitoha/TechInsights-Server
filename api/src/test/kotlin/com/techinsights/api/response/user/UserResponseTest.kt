package com.techinsights.api.response.user

import com.techinsights.domain.enums.UserRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UserResponseTest : FunSpec({

    test("UserResponse 생성 - 모든 필드 포함") {
        // when
        val response = UserResponse(
            id = 1L,
            email = "test@example.com",
            name = "Test User",
            nickname = "testuser",
            profileImage = "https://example.com/profile.png",
            role = UserRole.USER
        )

        // then
        response.id shouldBe 1L
        response.email shouldBe "test@example.com"
        response.name shouldBe "Test User"
        response.nickname shouldBe "testuser"
        response.profileImage shouldBe "https://example.com/profile.png"
        response.role shouldBe UserRole.USER
    }

    test("UserResponse 생성 - profileImage가 null") {
        // when
        val response = UserResponse(
            id = 2L,
            email = "noimage@example.com",
            name = "No Image User",
            nickname = "noimage",
            profileImage = null,
            role = UserRole.USER
        )

        // then
        response.id shouldBe 2L
        response.profileImage shouldBe null
        response.role shouldBe UserRole.USER
    }

    test("UserResponse 생성 - ADMIN 역할") {
        // when
        val response = UserResponse(
            id = 3L,
            email = "admin@example.com",
            name = "Admin User",
            nickname = "admin",
            profileImage = "https://example.com/admin.png",
            role = UserRole.ADMIN
        )

        // then
        response.id shouldBe 3L
        response.role shouldBe UserRole.ADMIN
    }

    test("UserResponse data class - equals 동작 확인") {
        // given
        val response1 = UserResponse(
            id = 1L,
            email = "test@example.com",
            name = "Test User",
            nickname = "testuser",
            profileImage = "image.png",
            role = UserRole.USER
        )

        val response2 = UserResponse(
            id = 1L,
            email = "test@example.com",
            name = "Test User",
            nickname = "testuser",
            profileImage = "image.png",
            role = UserRole.USER
        )

        // then
        response1 shouldBe response2
    }

    test("UserResponse data class - copy 동작 확인") {
        // given
        val original = UserResponse(
            id = 1L,
            email = "original@example.com",
            name = "Original Name",
            nickname = "original",
            profileImage = "original.png",
            role = UserRole.USER
        )

        // when
        val copied = original.copy(
            name = "Updated Name",
            nickname = "updated"
        )

        // then
        copied.id shouldBe 1L
        copied.email shouldBe "original@example.com"
        copied.name shouldBe "Updated Name"
        copied.nickname shouldBe "updated"
        copied.profileImage shouldBe "original.png"
        copied.role shouldBe UserRole.USER
    }

    test("UserResponse - 다양한 이메일 형식") {
        // given
        val emails = listOf(
            "user@example.com",
            "user.name@example.co.kr",
            "user+tag@example.com",
            "user123@sub.example.com"
        )

        emails.forEachIndexed { index, email ->
            // when
            val response = UserResponse(
                id = index.toLong(),
                email = email,
                name = "User $index",
                nickname = "user$index",
                profileImage = null,
                role = UserRole.USER
            )

            // then
            response.email shouldBe email
        }
    }

    test("UserResponse - 다양한 닉네임 형식") {
        // given
        val nicknames = listOf(
            "user123",
            "user_name",
            "user-name",
            "가나다라",
            "User名前"
        )

        nicknames.forEachIndexed { index, nickname ->
            // when
            val response = UserResponse(
                id = index.toLong(),
                email = "user$index@example.com",
                name = "User $index",
                nickname = nickname,
                profileImage = null,
                role = UserRole.USER
            )

            // then
            response.nickname shouldBe nickname
        }
    }

    test("UserResponse - hashCode 일관성") {
        // given
        val response1 = UserResponse(
            id = 1L,
            email = "test@example.com",
            name = "Test",
            nickname = "test",
            profileImage = null,
            role = UserRole.USER
        )

        val response2 = UserResponse(
            id = 1L,
            email = "test@example.com",
            name = "Test",
            nickname = "test",
            profileImage = null,
            role = UserRole.USER
        )

        // then
        response1.hashCode() shouldBe response2.hashCode()
    }

    test("UserResponse - toString 포함 확인") {
        // given
        val response = UserResponse(
            id = 1L,
            email = "test@example.com",
            name = "Test User",
            nickname = "testuser",
            profileImage = "image.png",
            role = UserRole.USER
        )

        // when
        val toString = response.toString()

        // then
        toString.contains("id=1") shouldBe true
        toString.contains("email=test@example.com") shouldBe true
        toString.contains("nickname=testuser") shouldBe true
    }
})
