package com.techinsights.batch.common.reader.base

import com.techinsights.domain.utils.Tsid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamReader
import java.time.LocalDateTime

abstract class AbstractCursorBasedReader<T> : ItemStreamReader<T> {

    protected val log: Logger = LoggerFactory.getLogger(javaClass)

    protected var lastPublishedAt: LocalDateTime? = null
    protected var lastId: Long? = null
    protected var readCount: Long = 0L

    protected abstract fun getContextKeyPrefix(): String

    protected abstract fun getMaxCount(): Long

    protected fun hasReachedLimit(): Boolean = readCount >= getMaxCount()

    protected fun calculateRemaining(): Long = getMaxCount() - readCount

    protected fun updateCursorFromTsidId(tsidId: String) {
        lastId = Tsid.decode(tsidId)
    }

    protected fun updateCursor(publishedAt: LocalDateTime, tsidId: String) {
        lastPublishedAt = publishedAt
        lastId = Tsid.decode(tsidId)
    }

    override fun open(executionContext: ExecutionContext) {
        val publishedAtKey = "${getContextKeyPrefix()}.cursor.publishedAt"
        val idKey = "${getContextKeyPrefix()}.cursor.id"
        val readCountKey = "${getContextKeyPrefix()}.readCount"

        if (executionContext.containsKey(publishedAtKey) && executionContext.containsKey(idKey)) {
            val savedPublishedAt = executionContext.getString(publishedAtKey)
            val savedId = executionContext.getLong(idKey)

            lastPublishedAt = LocalDateTime.parse(savedPublishedAt)
            lastId = savedId
            log.info("Resumed from cursor: publishedAt=$lastPublishedAt, id=$lastId")
        } else {
            log.info("Starting from beginning (no cursor found)")
        }

        readCount = executionContext.getLong(readCountKey, 0L)
        log.info("Initial read count: $readCount")
    }

    override fun update(executionContext: ExecutionContext) {
        val publishedAtKey = "${getContextKeyPrefix()}.cursor.publishedAt"
        val idKey = "${getContextKeyPrefix()}.cursor.id"
        val readCountKey = "${getContextKeyPrefix()}.readCount"

        lastPublishedAt?.let {
            executionContext.putString(publishedAtKey, it.toString())
        }
        lastId?.let {
            executionContext.putLong(idKey, it)
        }
        executionContext.putLong(readCountKey, readCount)
    }

    override fun close() {
        log.info(
            "Closed reader: processed $readCount/${getMaxCount()} items " +
            "(final cursor: publishedAt=$lastPublishedAt, id=$lastId)"
        )
    }
}
