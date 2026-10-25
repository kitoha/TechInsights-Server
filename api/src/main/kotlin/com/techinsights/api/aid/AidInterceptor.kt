package com.techinsights.api.aid

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AidInterceptor(private val manager: AidManager): HandlerInterceptor{
  override fun preHandle(req: HttpServletRequest, res: HttpServletResponse, h: Any): Boolean {
    if (isBot(req) || req.method == "OPTIONS") return true
    req.setAttribute(AID_ATTRIBUTE, manager.ensure(req, res))
    return true
  }
  private fun isBot(req: HttpServletRequest): Boolean {
    val ua = req.getHeader("User-Agent")?.lowercase() ?: return false
    return listOf("bot","spider","crawler","preview","slurp").any(ua::contains)
  }

  companion object {
    /** Holds the aid of the current request, including one issued just now whose cookie the request does not carry yet. */
    const val AID_ATTRIBUTE = "com.techinsights.api.aid"
  }

}
