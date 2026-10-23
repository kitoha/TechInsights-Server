package com.techinsights.api.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

/**
 * nginx is the only trusted proxy: it resolves the real client IP (Cloudflare's CF-Connecting-IP)
 * and overwrites X-Real-IP with a single value. X-Forwarded-For is client-controllable, so it is ignored.
 * The IP is for logging / rate limiting only, never an identity.
 */
@Component
object ClientIpExtractor {
  fun extract(request: HttpServletRequest): String {
    val xRealIp = request.getHeader("X-Real-IP")
    if (!xRealIp.isNullOrBlank()) {
      return xRealIp.trim()
    }

    return request.remoteAddr
  }
}
