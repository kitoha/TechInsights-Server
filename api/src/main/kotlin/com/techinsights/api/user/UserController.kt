package com.techinsights.api.user

import com.techinsights.api.auth.CustomUserDetails
import com.techinsights.domain.service.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    /**
     * 내 정보 조회
     *
     * @param userDetails 인증된 사용자 정보
     * @return 사용자 정보
     */
    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal userDetails: CustomUserDetails?): ResponseEntity<UserResponse> {
        if (userDetails == null) {
            return ResponseEntity.status(401).build()
        }

        val user = userService.getUserProfileById(userDetails.userId)
        return ResponseEntity.ok(UserResponse.from(user))
    }

    /**
     * 내 닉네임 변경
     * @param userDetails 인증된 사용자 정보
     * @param request 변경할 닉네임 정보
     * @return 변경된 사용자 정보
     */
    @PostMapping("/me/nickname")
    fun updateMyNickname(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestBody request: UpdateNicknameRequest
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.updateNicknameProfile(
            userId = userDetails.userId,
            newNickname = request.nickname
        )

        return ResponseEntity.ok(UserResponse.from(updatedUser))
    }
}
