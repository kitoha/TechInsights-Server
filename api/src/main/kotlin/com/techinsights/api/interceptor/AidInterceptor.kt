package com.techinsights.api.interceptor

import com.techinsights.api.service.aid.AidManager
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AidInterceptor(private val manager: AidManager): HandlerInterceptor{
  override fun preHandle(req: HttpServletRequest, res: HttpServletResponse, h: Any): Boolean {
    if (isBot(req) || req.method == "OPTIONS") return true
    manager.ensure(req, res)
    return true
  }
  private fun isBot(req: HttpServletRequest): Boolean {
    val ua = req.getHeader("User-Agent")?.lowercase() ?: return false
    return listOf("bot","spider","crawler","preview","slurp").any(ua::contains)
  }

}