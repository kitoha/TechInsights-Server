package com.techinsights.domain.service.gemini

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techinsights.domain.dto.gemini.SummaryResultWithId
import org.slf4j.LoggerFactory

class StreamingJsonParser {
    companion object {
        private const val OBJECT_START = '{'
        private const val OBJECT_END = '}'
        private const val QUOTE = '"'
        private const val ESCAPE = '\u005C'
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
    private var buffer = ""

    fun process(chunk: String): List<SummaryResultWithId> {
        buffer += chunk
        val summaries = mutableListOf<SummaryResultWithId>()

        while (true) {
            val startIndex = buffer.indexOf(OBJECT_START)
            if (startIndex == -1) break

            val endIndex = findEndOfObject(startIndex)
            if (endIndex != -1) {
                val jsonObject = buffer.substring(startIndex, endIndex + 1)
                val summary = parseSummary(jsonObject)
                
                if (summary != null) {
                    summaries.add(summary)
                    buffer = buffer.substring(endIndex + 1)
                } else {
                    buffer = buffer.substring(startIndex + 1)
                }
            } else {
                break
            }
        }
        return summaries
    }

    private fun findEndOfObject(startIndex: Int): Int {
        var depth = 0
        var isInsideQuotes = false
        var isEscaped = false

        for (i in startIndex until buffer.length) {
            val char = buffer[i]

            if (char == QUOTE && !isEscaped) {
                isInsideQuotes = !isInsideQuotes
            }

            if (!isInsideQuotes) {
                when (char) {
                    OBJECT_START -> depth++
                    OBJECT_END -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }

            isEscaped = (char == ESCAPE && !isEscaped)
        }
        return -1
    }

    private fun parseSummary(json: String): SummaryResultWithId? {
        return try {
            val result = mapper.readValue(json, SummaryResultWithId::class.java)
            if (result.id.isNullOrEmpty()) {
                null
            } else {
                log.info("Successfully parsed summary for ID: ${result.id}")
                result
            }
        } catch (e: Exception) {
            null
        }
    }
}
