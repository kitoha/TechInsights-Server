package com.techinsights.api.auth

import com.techinsights.domain.exception.user.AccountDeactivatedException
import com.techinsights.domain.service.user.UserService
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userService: UserService
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        return processOAuth2User(oAuth2User)
    }

    internal fun processOAuth2User(oAuth2User: OAuth2User): OAuth2User {
        val attributes = oAuth2User.attributes
        val providerId = attributes["sub"] as? String ?: throw invalidUserInfo()
        val email = attributes["email"] as? String ?: throw invalidUserInfo()
        val name = attributes["name"] as? String ?: throw invalidUserInfo()
        val profileImage = attributes["picture"] as? String

        val authUser = try {
            userService.upsertGoogleOAuthUser(
                providerId = providerId,
                email = email,
                name = name,
                profileImage = profileImage
            )
        } catch (e: AccountDeactivatedException) {
            throw OAuth2AuthenticationException(OAuth2Error(ACCOUNT_DEACTIVATED_ERROR_CODE), e.message, e)
        } catch (e: DataIntegrityViolationException) {
            if (!isEmailUniqueViolation(e)) throw e
            throw OAuth2AuthenticationException(OAuth2Error(EMAIL_CONFLICT_ERROR_CODE), "이미 다른 계정이 사용 중인 이메일입니다.", e)
        }
        
        return CustomUserDetails(
            userId = authUser.id,
            email = authUser.email,
            role = authUser.role,
            attributes = attributes
        )
    }

    private fun invalidUserInfo() =
        OAuth2AuthenticationException(OAuth2Error(INVALID_USER_INFO_ERROR_CODE), "OAuth2 provider returned incomplete user info")

    private fun isEmailUniqueViolation(e: DataIntegrityViolationException): Boolean =
        generateSequence<Throwable>(e) { it.cause }
            .filterIsInstance<ConstraintViolationException>()
            .any { it.constraintName == USERS_EMAIL_CONSTRAINT }

    companion object {
        const val ACCOUNT_DEACTIVATED_ERROR_CODE = "account_deactivated"
        const val EMAIL_CONFLICT_ERROR_CODE = "email_conflict"
        const val INVALID_USER_INFO_ERROR_CODE = "invalid_user_info"
        private const val USERS_EMAIL_CONSTRAINT = "users_email_key"
    }
}
