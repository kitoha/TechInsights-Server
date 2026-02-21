package com.techinsights.api.aid

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service

@Service
class AidManager (
  private val gen: AidGenerator,
  private val store: AidService,
  private val props: AidProperties
){
  fun ensure(request: HttpServletRequest, response: HttpServletResponse): String =
    store.read(request, props) ?: gen.generate().also { store.write(response, request, props, it) }
}
