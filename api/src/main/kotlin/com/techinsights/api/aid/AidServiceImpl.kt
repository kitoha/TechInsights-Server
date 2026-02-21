package com.techinsights.api.aid

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service

@Service
class AidServiceImpl(private val policy: CookiePolicy) : AidService {

  override fun read(
    req: HttpServletRequest,
    p: AidProperties
  ): String? = req.cookies?.firstOrNull { it.name == p.cookie.name }?.value?.takeIf { it.isNotBlank() }

  override fun write(res: HttpServletResponse, req: HttpServletRequest, p: AidProperties, value: String) {
    val builder = ResponseCookie
      .from(p.cookie.name, value)
      .httpOnly(true)
      .secure(policy.secure(req, p))
      .path("/")
      .maxAge(java.time.Duration.ofDays(p.cookie.ttlDays))
      .sameSite(policy.sameSite(req, p))
    p.cookie.domain?.let(builder::domain)
    res.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString())
  }
}
