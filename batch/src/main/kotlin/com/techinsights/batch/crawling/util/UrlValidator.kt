package com.techinsights.batch.crawling.util

import org.springframework.stereotype.Component
import java.net.Inet6Address
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
            val addresses = InetAddress.getAllByName(host)
            addresses.none { isPrivateOrReserved(it) }
        }.getOrDefault(false)
    }

    private fun isPrivateOrReserved(address: InetAddress): Boolean {
        if (address.isLoopbackAddress) return true
        if (address.isSiteLocalAddress) return true
        if (address.isLinkLocalAddress) return true
        if (address.isAnyLocalAddress) return true
        if (address.isMulticastAddress) return true

        if (address is Inet6Address) {
            val firstByte = address.address[0].toInt() and 0xFF
            if (firstByte and 0xFE == 0xFC) return true
        }
        return false
    }
}
