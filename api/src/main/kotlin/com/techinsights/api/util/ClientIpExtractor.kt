package com.techinsights.api.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
object ClientIpExtractor {
  fun extract(request: HttpServletRequest): String {

    val xForwardedFor = request.getHeader("X-Forwarded-For")
    if (!xForwardedFor.isNullOrBlank()) {
      return xForwardedFor.split(",")[0].trim()
    }

    val xRealIp = request.getHeader("X-Real-IP")
    if (!xRealIp.isNullOrBlank()) {
      return xRealIp
    }

    return request.remoteAddr
  }
}
