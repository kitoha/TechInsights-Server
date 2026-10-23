package com.techinsights.api.exception

import com.fasterxml.jackson.databind.json.JsonMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.ProblemDetailJacksonMixin
import java.net.URI
import java.time.OffsetDateTime

/**
 * Single error contract of the API: RFC 7807 problem details (`application/problem+json`).
 * Extension members: `code` (stable machine-readable error code), `timestamp`, and `errors` (field -> message, validation only).
 */
object ProblemDetails {

    private val mapper = JsonMapper.builder()
        .findAndAddModules()
        .addMixIn(ProblemDetail::class.java, ProblemDetailJacksonMixin::class.java)
        .build()

    fun create(
        status: HttpStatus,
        code: String,
        detail: String,
        path: String? = null,
        errors: Map<String, Any>? = null
    ): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(status, detail)
        problem.title = status.reasonPhrase
        path?.let { problem.instance = runCatching { URI(null, null, it, null) }.getOrNull() }
        problem.setProperty("code", code)
        problem.setProperty("timestamp", OffsetDateTime.now().toString())
        errors?.let { problem.setProperty("errors", it) }
        return problem
    }

    fun response(
        status: HttpStatus,
        code: String,
        detail: String,
        path: String? = null,
        errors: Map<String, Any>? = null,
        headers: HttpHeaders = HttpHeaders.EMPTY
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status)
            .headers(headers)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(create(status, code, detail, path, errors))

    /** For servlet-level handlers (security entry point / access denied) that run outside Spring MVC. */
    fun write(response: HttpServletResponse, status: HttpStatus, code: String, detail: String, path: String?) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(mapper.writeValueAsString(create(status, code, detail, path)))
    }
}
