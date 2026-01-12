package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techinsights.domain.dto.gemini.ArticleInput
import org.springframework.stereotype.Component

@Component
class BatchPromptBuilder {

    private val mapper = jacksonObjectMapper()

    fun buildPrompt(articles: List<ArticleInput>, categories: List<String>): String {
        return """
당신은 기술 블로그 글을 요약하는 전문가입니다.
다음 ${articles.size}개의 글을 각각 요약해주세요.

**중요: 각 결과에는 반드시 원본 글의 ID를 포함해야 합니다.**

${articles.mapIndexed { index, article ->
    """
=== 글 ${index + 1} ===
ID: ${article.id}
제목: ${article.title}
내용:
${article.content}
"""
}.joinToString("\n\n")}

**응답 형식:**
다음 JSON 형식으로 응답해주세요. 각 결과는 반드시 ID를 포함해야 합니다.

{
  "results": [
    {
      "id": "원본 글의 ID",
      "success": true,
      "summary": "3-5문장으로 핵심 내용 요약",
      "categories": ["카테고리1", "카테고리2"],
      "preview": "본문의 첫 100자 정도를 발췌"
    }
  ]
}

**규칙:**
1. 각 글마다 별도의 결과 객체 생성
2. ID는 원본 글의 ID를 정확히 복사
3. 요약이 불가능한 경우 success: false로 표시하고 error 필드에 이유 설명
4. 카테고리는 다음 중에서만 선택: ${categories.joinToString(", ")}
5. summary는 핵심 내용을 명확하게 3-5문장으로 요약
6. preview는 원문의 첫 100자 정도를 그대로 발췌
""".trimIndent()
    }

    fun buildSchema(categories: List<String>): String {
        return """
{
  "type": "object",
  "properties": {
    "results": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "id": {"type": "string"},
          "success": {"type": "boolean"},
          "summary": {"type": "string"},
          "categories": {
            "type": "array",
            "items": {
              "type": "string",
              "enum": ${mapper.writeValueAsString(categories)}
            }
          },
          "preview": {"type": "string"},
          "error": {"type": "string"}
        },
        "required": ["id", "success"]
      }
    }
  },
  "required": ["results"]
}
""".trimIndent()
    }
}
