package com.techinsights.domain.convention

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * AGENTS.md: "Never use fully qualified class names inline — always add an import statement."
 * 한 곳(domain)에서 api/domain/batch의 main·test Kotlin 소스를 모두 스캔해, 모듈별 중복 없이 `./gradlew test`에서 항상 검사한다.
 */
class InlineFqnConventionTest : FunSpec({

    context("scanner") {
        test("코드의 인라인 FQN을 줄 번호와 함께 찾는다") {
            val source = """
                package a.b

                import java.util.UUID

                class A {
                    val id = java.util.UUID.randomUUID()
                    fun f(): org.springframework.http.MediaType? = null
                }
            """.trimIndent()

            InlineFqnScanner.scan(source) shouldBe listOf(
                6 to "java.util.UUID",
                7 to "org.springframework.http.MediaType"
            )
        }

        test("package와 import 줄, 문자열, 주석은 무시한다") {
            val source = "package com.techinsights.domain.dto.post\n" +
                "import java.util.UUID\n" +
                "import org.slf4j.LoggerFactory as Factory\n" +
                "// java.util.UUID in a line comment\n" +
                "/* org.jsoup.Jsoup in a block comment */\n" +
                "/** KDoc mentions [java.time.Duration] */\n" +
                "val s = \"java.util.UUID in a string\"\n" +
                "val raw = \"\"\"\n  org.jsoup.nodes.Element in raw string\n\"\"\"\n"

            InlineFqnScanner.scan(source).shouldBeEmpty()
        }

        test("패키지 경로가 아닌 프로퍼티 접근은 FQN으로 보지 않는다") {
            val source = "val x = properties.cookie.Domain\nval y = request.user.Name\n"

            InlineFqnScanner.scan(source).shouldBeEmpty()
        }
    }

    test("api, domain, batch의 main/test 소스에 인라인 FQN이 없다") {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").exists() }

        val violations = listOf("api", "domain", "batch")
            .map { File(root, "$it/src") }
            .filter { it.exists() }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .flatMap { file ->
                InlineFqnScanner.scan(file.readText()).map { (line, fqn) ->
                    "${file.relativeTo(root).path}:$line  $fqn"
                }
            }

        withClue("Inline fully qualified names found (use imports):\n${violations.joinToString("\n")}\n") {
            violations.shouldBeEmpty()
        }
    }
})

private object InlineFqnScanner {

    private val ROOT_PACKAGES = listOf(
        "com", "org", "io", "java", "javax", "jakarta", "kotlin", "kotlinx", "net", "mu", "reactor", "software"
    )
    private val FQN = Regex(
        "(?<![\\w.])((?:${ROOT_PACKAGES.joinToString("|")})(?:\\.[a-z_][a-z0-9_]*)+\\.[A-Z]\\w*)"
    )
    private val DECLARATION_LINE = Regex("^\\s*(package|import)\\s")

    fun scan(source: String): List<Pair<Int, String>> {
        val code = blankOutCommentsAndStrings(source.replace("\r\n", "\n"))
        return code.split('\n').withIndex().flatMap { (index, line) ->
            if (DECLARATION_LINE.containsMatchIn(line)) emptyList()
            else FQN.findAll(line).map { (index + 1) to it.groupValues[1] }.toList()
        }
    }

    private fun blankOutCommentsAndStrings(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0

        fun blank(from: Int, to: Int) {
            for (k in from until to) out.append(if (source[k] == '\n') '\n' else ' ')
        }

        while (i < source.length) {
            when {
                source.startsWith("//", i) -> {
                    val end = source.indexOf('\n', i).let { if (it < 0) source.length else it }
                    blank(i, end); i = end
                }
                source.startsWith("/*", i) -> {
                    val end = source.indexOf("*/", i + 2).let { if (it < 0) source.length else it + 2 }
                    blank(i, end); i = end
                }
                source.startsWith("\"\"\"", i) -> {
                    val end = source.indexOf("\"\"\"", i + 3).let { if (it < 0) source.length else it + 3 }
                    blank(i, end); i = end
                }
                source[i] == '"' -> {
                    var end = i + 1
                    while (end < source.length && source[end] != '"') end += if (source[end] == '\\') 2 else 1
                    end = minOf(end + 1, source.length)
                    blank(i, end); i = end
                }
                else -> { out.append(source[i]); i++ }
            }
        }
        return out.toString()
    }
}
