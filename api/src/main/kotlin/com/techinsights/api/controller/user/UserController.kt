package com.techinsights.api.controller.user

import com.techinsights.api.response.user.UserResponse
import com.techinsights.api.util.auth.CustomUserDetails
import com.techinsights.domain.repository.user.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userRepository: UserRepository
) {

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal userDetails: CustomUserDetails?): ResponseEntity<UserResponse> {
        if (userDetails == null) {
            return ResponseEntity.status(401).build()
        }

        val user = userRepository.findById(userDetails.userId)
            .orElseThrow { RuntimeException("User not found") }

        return ResponseEntity.ok(
            UserResponse(
                id = user.id,
                email = user.email,
                name = user.name,
                nickname = user.nickname,
                profileImage = user.profileImage,
                role = user.role
            )
        )
    }
}
