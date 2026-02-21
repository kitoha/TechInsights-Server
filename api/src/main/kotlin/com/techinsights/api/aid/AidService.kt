package com.techinsights.api.aid

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

interface AidService {
  fun read(req: HttpServletRequest, p: AidProperties): String?
  fun write(res: HttpServletResponse, req: HttpServletRequest, p: AidProperties, value: String)
}
