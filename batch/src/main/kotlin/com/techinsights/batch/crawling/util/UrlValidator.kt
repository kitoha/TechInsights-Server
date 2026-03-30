package com.techinsights.batch.crawling.util

import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI

@Component
class UrlValidator {

    private val allowedSchemes = setOf("http", "https")

    fun isSafe(url: String): Boolean {
        if (url.isBlank()) return false
        return runCatching {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme !in allowedSchemes) return false
            val host = uri.host ?: return false
            val address = InetAddress.getByName(host)
            !isPrivateOrReserved(address)
        }.getOrDefault(false)
    }

    private fun isPrivateOrReserved(address: InetAddress): Boolean {
        return address.isLoopbackAddress
            || address.isSiteLocalAddress
            || address.isLinkLocalAddress
            || address.isAnyLocalAddress
            || address.isMulticastAddress
    }
}
