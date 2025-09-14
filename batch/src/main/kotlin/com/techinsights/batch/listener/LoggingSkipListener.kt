package com.techinsights.batch.listener

import com.techinsights.domain.dto.post.PostDto
import org.slf4j.LoggerFactory
import org.springframework.batch.core.SkipListener
import org.springframework.stereotype.Component

@Component
class LoggingSkipListener : SkipListener<Any, Any> {

    private val log = LoggerFactory.getLogger(LoggingSkipListener::class.java)

    override fun onSkipInRead(t: Throwable) {
        log.warn("Skipped item in reader due to: ${t.message}")
    }

    override fun onSkipInWrite(item: Any, t: Throwable) {
        val itemId = getItemId(item)
        log.warn("Skipped item in writer with id=$itemId due to: ${t.message}")
    }

    override fun onSkipInProcess(item: Any, t: Throwable) {
        val itemId = getItemId(item)
        log.warn("Skipped item in processor with id=$itemId due to: ${t.message}")
    }

    private fun getItemId(item: Any): String {
        return when (item) {
            is PostDto -> item.id
            else -> "unknown"
        }
    }
}
