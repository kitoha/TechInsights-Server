package com.techinsights.api.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
object ClientIpExtractor {
  fun extract(request: HttpServletRequest): String {
    return request.remoteAddr
  }
}
