package com.techinsights.batch.infrastructure.config

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationState
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.stereotype.Component

/**
 * 배치는 스키마를 마이그레이션하지 않는다(API가 담당). 시작 시 DB 스키마가 코드와 일치하는지만 검증하고,
 * 뒤처졌거나(미적용) 앞서 있으면(코드에 없는 적용 이력) 즉시 실패한다.
 */
@Component
class ValidateOnlyFlywayMigrationStrategy : FlywayMigrationStrategy {

    override fun migrate(flyway: Flyway) {
        flyway.validate()

        val ahead = flyway.info().all().filter { it.state in AHEAD_STATES }
        check(ahead.isEmpty()) {
            "DB schema is ahead of the batch code: ${ahead.map { it.version }}"
        }
    }

    companion object {
        private val AHEAD_STATES = setOf(MigrationState.FUTURE_SUCCESS, MigrationState.FUTURE_FAILED)
    }
}
