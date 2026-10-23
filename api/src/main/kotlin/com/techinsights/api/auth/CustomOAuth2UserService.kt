package com.techinsights.api.auth

import com.techinsights.domain.exception.user.AccountDeactivatedException
import com.techinsights.domain.service.user.UserService
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
        val providerId = attributes["sub"] as String
        val email = attributes["email"] as String
        val name = attributes["name"] as String
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
        }
        
        return CustomUserDetails(
            userId = authUser.id,
            email = authUser.email,
            role = authUser.role,
            attributes = attributes
        )
    }

    companion object {
        const val ACCOUNT_DEACTIVATED_ERROR_CODE = "account_deactivated"
    }
}
