package com.techinsights.api.response.auth

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)
