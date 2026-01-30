package com.techinsights.api.service.auth

import com.techinsights.domain.repository.user.UserRepository
import com.techinsights.domain.entity.user.User
import com.techinsights.domain.utils.Tsid
import com.techinsights.api.util.auth.CustomUserDetails
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

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
        val googleSub = attributes["sub"] as String
        val email = attributes["email"] as String
        val name = attributes["name"] as String
        val profileImage = attributes["picture"] as? String

        val user = userRepository.findByGoogleSub(googleSub)
            .map { existingUser ->
                existingUser.apply {
                    this.name = name
                    this.profileImage = profileImage
                }
            }
            .orElseGet {
                User(
                    id = Tsid.decode(Tsid.generate()),
                    email = email,
                    name = name,
                    googleSub = googleSub,
                    profileImage = profileImage
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
