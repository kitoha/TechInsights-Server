package com.techinsights.domain.entity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import jakarta.persistence.Entity
import org.hibernate.annotations.SQLRestriction
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter

/**
 * Hibernate 6.6 does not inherit @SQLRestriction from a @MappedSuperclass, so every concrete entity extending
 * [BaseEntity] must declare it itself or soft-deleted rows leak into every query.
 */
class SoftDeleteRestrictionConventionTest : FunSpec({

    test("BaseEntity를 상속한 모든 엔티티는 @SQLRestriction(deleted_at IS NULL)을 직접 선언한다") {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(Entity::class.java))
        val entities = scanner.findCandidateComponents("com.techinsights.domain.entity")
            .map { Class.forName(it.beanClassName) }
            .filter { BaseEntity::class.java.isAssignableFrom(it) }

        entities.shouldNotBeEmpty()
        entities
            .filter { it.getAnnotation(SQLRestriction::class.java)?.value != "deleted_at IS NULL" }
            .map { it.simpleName }
            .shouldBeEmpty()
    }
})
