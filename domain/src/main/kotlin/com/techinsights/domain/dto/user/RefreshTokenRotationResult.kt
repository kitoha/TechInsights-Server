package com.techinsights.domain.dto.user

sealed interface RefreshTokenRotationResult {
    data object Rotated : RefreshTokenRotationResult
    /** The just-retired token was presented within grace: issue an access token only, no rotation. */
    data object GraceAccessOnly : RefreshTokenRotationResult
    data object Expired : RefreshTokenRotationResult
    data object ReuseDetected : RefreshTokenRotationResult
    data object NotFound : RefreshTokenRotationResult
}
