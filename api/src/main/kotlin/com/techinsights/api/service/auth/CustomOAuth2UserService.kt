package com.techinsights.api.service.auth

import com.techinsights.domain.repository.user.UserRepository
import com.techinsights.domain.entity.user.User
import com.techinsights.domain.utils.Tsid
import com.techinsights.api.util.auth.CustomUserDetails
import com.techinsights.domain.enums.ProviderType
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository
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

        val user = userRepository.findByProviderAndProviderId(ProviderType.GOOGLE, providerId)
            .map { existingUser ->
                existingUser.apply {
                    this.name = name
                    this.profileImage = profileImage
                    this.login()
                }
            }
            .orElseGet {
                val randomNickname = "User_${Tsid.generate()}"
                User(
                    id = Tsid.decode(Tsid.generate()),
                    email = email,
                    name = name,
                    nickname = randomNickname,
                    provider = ProviderType.GOOGLE,
                    providerId = providerId,
                    profileImage = profileImage,
                    lastLoginAt = LocalDateTime.now()
                )
            }

        val savedUser = userRepository.save(user)
        
        return CustomUserDetails(
            userId = savedUser.id,
            email = savedUser.email,
            role = savedUser.role,
            attributes = attributes
        )
    }
}
