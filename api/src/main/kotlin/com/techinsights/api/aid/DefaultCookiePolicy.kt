package com.techinsights.api.aid

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.net.URI

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
    val originHeader = request.getHeader("Origin")?.takeIf { it.isNotBlank() } ?: return props.cookie.sameSiteDefault
    val requestHost = extractRequestHost(request) ?: return props.cookie.sameSiteDefault
    val originHost = extractOriginHost(originHeader) ?: return props.cookie.sameSiteDefault

    return if (isCrossSite(requestHost, originHost) && secure(request, props)) {
      "None"
    } else {
      props.cookie.sameSiteDefault
    }
  }

  private fun extractRequestHost(req: HttpServletRequest): String? {
    val forwardedHost = req.getHeader("X-Forwarded-Host")
      ?.substringBefore(",")
      ?.trim()
      ?.substringBefore(":")
      ?.lowercase()
      ?.takeIf { it.isNotBlank() }

    return forwardedHost ?: req.serverName
      ?.substringBefore(":")
      ?.lowercase()
      ?.takeIf { it.isNotBlank() }
  }

  private fun extractOriginHost(origin: String): String? {
    return runCatching {
      URI(origin).host?.lowercase()?.takeIf { it.isNotBlank() }
    }.getOrNull()
  }

  private fun isCrossSite(requestHost: String, originHost: String): Boolean {
    return !(isSameHostOrSubdomain(requestHost, originHost) || isSameHostOrSubdomain(originHost, requestHost))
  }

  private fun isSameHostOrSubdomain(parentHost: String, childHost: String): Boolean {
    return childHost == parentHost || childHost.endsWith(".$parentHost")
  }
}
