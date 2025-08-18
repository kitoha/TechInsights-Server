package com.techinsights.api.policy

import com.techinsights.api.config.aid.CookiePolicy
import com.techinsights.api.props.AidProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class DefaultCookiePolicy : CookiePolicy{

  override fun secure(
    request: HttpServletRequest,
    props: AidProperties
  ): Boolean = when(props.cookie.secureMode){
      AidProperties.SecureMode.ALWAYS -> true
      AidProperties.SecureMode.NEVER -> false
      AidProperties.SecureMode.AUTO ->
        request.isSecure || request.getHeader("X-Forwarded-Proto")?.equals("https", true) == true
    }

  override fun sameSite(
    request: HttpServletRequest,
    props: AidProperties
  ): String {
    val origin  = request.getHeader("Origin") ?: props.cookie.sameSiteDefault
    val sameSite = if(isCrossSite(request, origin)) "None" else props.cookie.sameSiteDefault
    return sameSite
  }

  private fun isCrossSite(req: HttpServletRequest, origin: String): Boolean {
    val host = req.getHeader("X-Forwarded-Host") ?: req.serverName
    return !origin.contains(host, ignoreCase = true)
  }
}