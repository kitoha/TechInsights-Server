package com.techinsights.domain.dto.auth

sealed interface Requester {
    val identifier: String
    val ip: String

    data class Authenticated(val userId: Long, override val ip: String) : Requester {
        override val identifier: String = userId.toString()
    }

    /**
     * @param carriedAidCookie true only when the request itself carried the aid cookie. An aid minted during this
     * request (or an IP fallback for bots) is not a proven identity: it must not be counted by write paths.
     */
    data class Anonymous(
        val anonymousId: String,
        override val ip: String,
        val carriedAidCookie: Boolean = true
    ) : Requester {
        override val identifier: String = anonymousId
    }
}
