package com.techinsights.api.config.aid

import com.techinsights.api.props.AidProperties
import jakarta.servlet.http.HttpServletRequest

interface CookiePolicy {
  fun secure(request: HttpServletRequest, props : AidProperties): Boolean
  fun sameSite(request: HttpServletRequest, props : AidProperties): String
}