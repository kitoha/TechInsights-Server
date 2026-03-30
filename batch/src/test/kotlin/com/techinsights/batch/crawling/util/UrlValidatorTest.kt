package com.techinsights.batch.crawling.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UrlValidatorTest : FunSpec({

    val validator = UrlValidator()

    context("안전한 URL") {
        test("일반 https URL은 허용되어야 한다") {
            validator.isSafe("https://techcrunch.com/feed") shouldBe true
        }
        test("일반 http URL은 허용되어야 한다") {
            validator.isSafe("http://example.com/rss") shouldBe true
        }
    }

    context("SSRF 위험 URL - 사설 IP 대역") {
        test("localhost는 차단되어야 한다") {
            validator.isSafe("http://localhost/admin") shouldBe false
        }
        test("127.0.0.1은 차단되어야 한다") {
            validator.isSafe("http://127.0.0.1/secret") shouldBe false
        }
        test("10.x.x.x 대역은 차단되어야 한다") {
            validator.isSafe("http://10.0.0.1/internal") shouldBe false
        }
        test("172.16.x.x 대역은 차단되어야 한다") {
            validator.isSafe("http://172.16.0.1/internal") shouldBe false
        }
        test("192.168.x.x 대역은 차단되어야 한다") {
            validator.isSafe("http://192.168.1.1/router") shouldBe false
        }
        test("AWS 메타데이터 엔드포인트 169.254.169.254는 차단되어야 한다") {
            validator.isSafe("http://169.254.169.254/latest/meta-data/") shouldBe false
        }
        test("IPv6 루프백 ::1은 차단되어야 한다") {
            validator.isSafe("http://[::1]/secret") shouldBe false
        }
    }

    context("비정상 URL") {
        test("빈 URL은 차단되어야 한다") {
            validator.isSafe("") shouldBe false
        }
        test("file:// 스킴은 차단되어야 한다") {
            validator.isSafe("file:///etc/passwd") shouldBe false
        }
        test("파싱 불가 URL은 차단되어야 한다") {
            validator.isSafe("not-a-url") shouldBe false
        }
    }
})
