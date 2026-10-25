package com.techinsights.domain.dto.user

sealed interface RefreshTokenRotationResult {
    data object Rotated : RefreshTokenRotationResult
    /**
     * The just-retired token was presented within the grace window (a retried / parallel refresh whose response was
     * lost or raced): no new rotation; the sealed successor stored at rotation time is re-delivered.
     */
    data class GraceRedelivery(val sealedSuccessor: String) : RefreshTokenRotationResult
    data object Expired : RefreshTokenRotationResult
    data object ReuseDetected : RefreshTokenRotationResult
    data object NotFound : RefreshTokenRotationResult
}
