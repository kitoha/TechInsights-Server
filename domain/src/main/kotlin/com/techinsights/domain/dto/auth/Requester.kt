package com.techinsights.domain.dto.auth

sealed interface Requester {
    val identifier: String
    val ip: String

    data class Authenticated(val userId: Long, override val ip: String) : Requester {
        override val identifier: String = userId.toString()
    }

    data class Anonymous(val anonymousId: String, override val ip: String) : Requester {
        override val identifier: String = anonymousId
    }
}
