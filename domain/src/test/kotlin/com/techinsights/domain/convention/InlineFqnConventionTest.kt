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

        test("문자 리터럴 '\"'를 문자열 시작으로 오인하지 않는다") {
            val source = "val quote = '\"'\n" +
                "val id = java.util.UUID.randomUUID()\n" +
                "val escaped = '\\\\'\n" +
                "val d = java.time.Duration.ZERO\n"

            InlineFqnScanner.scan(source) shouldBe listOf(
                2 to "java.util.UUID",
                4 to "java.time.Duration"
            )
        }

        test("문자열 템플릿 \${...} 안의 코드는 검사한다") {
            val source = "val a = \"id=\${java.util.UUID.randomUUID()}\"\n" +
                "val b = \"\"\"raw \${org.jsoup.Jsoup.parse(\"x\")} org.jsoup.nodes.Element in text\"\"\"\n" +
                "val c = \"nested \${map[\"k\"]} and java.util.Locale in text\"\n"

            InlineFqnScanner.scan(source) shouldBe listOf(
                1 to "java.util.UUID",
                2 to "org.jsoup.Jsoup"
            )
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

    /** Blanks comments, char literals and string text; code inside string templates `${...}` is kept. */
    private fun blankOutCommentsAndStrings(source: String): String = Blanker(source).run()

    private class Blanker(private val source: String) {
        private val out = StringBuilder(source.length)
        private var i = 0

        fun run(): String {
            scanCode(inTemplate = false)
            return out.toString()
        }

        private fun blank(from: Int, to: Int) {
            for (k in from until to) out.append(if (source[k] == '\n') '\n' else ' ')
        }

        private fun indexOrEnd(index: Int, skip: Int = 0): Int = if (index < 0) source.length else index + skip

        /** Scans code; when [inTemplate], returns (without consuming) at the `}` that closes the template. */
        private fun scanCode(inTemplate: Boolean) {
            var depth = 0
            while (i < source.length) {
                when {
                    source.startsWith("//", i) -> {
                        val end = indexOrEnd(source.indexOf('\n', i)); blank(i, end); i = end
                    }
                    source.startsWith("/*", i) -> {
                        val end = indexOrEnd(source.indexOf("*/", i + 2), 2); blank(i, end); i = end
                    }
                    source[i] == '\'' -> scanCharLiteral()
                    source.startsWith("\"\"\"", i) -> scanString(raw = true)
                    source[i] == '"' -> scanString(raw = false)
                    source[i] == '{' -> { depth++; out.append('{'); i++ }
                    source[i] == '}' -> {
                        if (inTemplate && depth == 0) return
                        depth--; out.append('}'); i++
                    }
                    else -> { out.append(source[i]); i++ }
                }
            }
        }

        private fun scanCharLiteral() {
            val end = when {
                source.startsWith("\\", i + 1) -> source.indexOf('\'', i + 3)
                i + 2 < source.length && source[i + 2] == '\'' -> i + 2
                else -> -1
            }
            if (end < 0) { out.append(source[i]); i++ } else { blank(i, end + 1); i = end + 1 }
        }

        private fun scanString(raw: Boolean) {
            val quote = if (raw) 3 else 1
            blank(i, i + quote); i += quote
            while (i < source.length) {
                when {
                    raw && source.startsWith("\"\"\"", i) -> { blank(i, i + 3); i += 3; return }
                    !raw && source[i] == '"' -> { blank(i, i + 1); i++; return }
                    !raw && source[i] == '\\' -> { val end = minOf(i + 2, source.length); blank(i, end); i = end }
                    source.startsWith("\${", i) -> {
                        blank(i, i + 2); i += 2
                        scanCode(inTemplate = true)
                        if (i < source.length) { blank(i, i + 1); i++ }
                    }
                    else -> { blank(i, i + 1); i++ }
                }
            }
        }
    }
}
