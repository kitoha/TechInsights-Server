package com.techinsights.api.aid

import jakarta.servlet.http.HttpServletRequest

interface CookiePolicy {
  fun secure(request: HttpServletRequest, props : AidProperties): Boolean
  fun sameSite(request: HttpServletRequest, props : AidProperties): String
}