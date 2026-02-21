package com.techinsights.api.auth

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
