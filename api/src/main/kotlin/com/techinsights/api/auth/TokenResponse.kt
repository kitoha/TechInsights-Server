package com.techinsights.api.auth

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

/** [refreshToken] is null when only a new access token is issued (parallel refresh inside the grace window). */
data class RefreshResponse(
    val accessToken: String,
    val refreshToken: String?
)
