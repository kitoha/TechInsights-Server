package com.techinsights.api.exception

import com.techinsights.domain.exception.PostNotFoundException
import io.kotest.core.spec.style.FunSpec
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class ProblemDetailContractTest : FunSpec({

    @RestController
    @Validated
    class ThrowingController {
        @GetMapping("/boom/not-found")
        fun notFound(): String = throw PostNotFoundException("missing")

        @GetMapping("/boom/illegal")
        fun illegal(): String = throw IllegalArgumentException("bad input")

        @GetMapping("/boom/unexpected")
        fun unexpected(): String = throw IllegalStateException("secret internals")
    }

    val mockMvc = MockMvcBuilders.standaloneSetup(ThrowingController())
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    test("도메인 예외는 problem+json 이며 안정적인 code 를 가진다") {
        mockMvc.get("/boom/not-found").andExpect {
            status { isNotFound() }
            content { contentTypeCompatibleWith("application/problem+json") }
            jsonPath("$.status") { value(404) }
            jsonPath("$.title") { value("Not Found") }
            jsonPath("$.code") { value("POST_NOT_FOUND") }
            jsonPath("$.detail") { value("게시글을 찾을 수 없습니다.") }
            jsonPath("$.instance") { value("/boom/not-found") }
            jsonPath("$.timestamp") { exists() }
        }
    }

    test("잘못된 인자는 400 INVALID_PARAMETER") {
        mockMvc.get("/boom/illegal").andExpect {
            status { isBadRequest() }
            content { contentTypeCompatibleWith("application/problem+json") }
            jsonPath("$.code") { value("INVALID_PARAMETER") }
            jsonPath("$.detail") { value("bad input") }
        }
    }

    test("예상치 못한 예외는 내부 정보 없이 500 INTERNAL_SERVER_ERROR") {
        mockMvc.get("/boom/unexpected").andExpect {
            status { isInternalServerError() }
            content { contentTypeCompatibleWith("application/problem+json") }
            jsonPath("$.code") { value("INTERNAL_SERVER_ERROR") }
            jsonPath("$.detail") { value("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.") }
        }
    }
})
