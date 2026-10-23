package com.techinsights.batch.infrastructure.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.MigrationInfoService
import org.flywaydb.core.api.MigrationState
import org.flywaydb.core.api.exception.FlywayValidateException

class ValidateOnlyFlywayMigrationStrategyTest : FunSpec({

    val strategy = ValidateOnlyFlywayMigrationStrategy()

    fun flywayWith(vararg states: MigrationState): Flyway {
        val flyway = mockk<Flyway>(relaxed = true)
        val infoService = mockk<MigrationInfoService>()
        every { infoService.all() } returns states.map { state ->
            mockk<MigrationInfo>(relaxed = true) { every { this@mockk.state } returns state }
        }.toTypedArray()
        every { flyway.info() } returns infoService
        return flyway
    }

    test("스키마가 코드와 일치하면 validate만 수행하고 migrate는 호출하지 않는다") {
        val flyway = flywayWith(MigrationState.SUCCESS, MigrationState.SUCCESS)

        strategy.migrate(flyway)

        verify(exactly = 1) { flyway.validate() }
        verify(exactly = 0) { flyway.migrate() }
    }

    test("적용되지 않은 마이그레이션이 있으면 validate 실패로 시작이 중단된다") {
        val flyway = flywayWith(MigrationState.SUCCESS, MigrationState.PENDING)
        every { flyway.validate() } throws FlywayValidateException(mockk(relaxed = true), "pending")

        shouldThrow<FlywayValidateException> { strategy.migrate(flyway) }

        verify(exactly = 0) { flyway.migrate() }
    }

    test("DB가 코드보다 앞서 있으면(코드에 없는 적용 이력) 시작이 중단된다") {
        val flyway = flywayWith(MigrationState.SUCCESS, MigrationState.FUTURE_SUCCESS)

        shouldThrow<IllegalStateException> { strategy.migrate(flyway) }

        verify(exactly = 0) { flyway.migrate() }
    }
})
