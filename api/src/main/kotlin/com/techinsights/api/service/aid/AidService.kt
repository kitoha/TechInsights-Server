package com.techinsights.api.service.aid

import com.techinsights.api.props.AidProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

interface AidService {
  fun read(req: HttpServletRequest, p: AidProperties): String?
  fun write(res: HttpServletResponse, req: HttpServletRequest, p: AidProperties, value: String)
}