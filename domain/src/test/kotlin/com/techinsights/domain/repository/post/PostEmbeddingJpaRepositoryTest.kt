package com.techinsights.domain.repository.post

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import org.springframework.data.jpa.repository.Query

class PostEmbeddingJpaRepositoryTest : FunSpec({

    val indexExpression = "embedding_vector::halfvec(3072) <=> CAST(:targetVector AS halfvec)"

    fun queryOf(methodName: String): String =
        PostEmbeddingJpaRepository::class.java.methods
            .first { it.name == methodName }
            .getAnnotation(Query::class.java)
            .value

    test("findSimilarPostsAll은 HNSW 인덱스와 같은 halfvec 표현식으로 정렬한다") {
        queryOf("findSimilarPostsAll") shouldContain indexExpression
    }

    test("findSimilarPostsExcluding은 HNSW 인덱스와 같은 halfvec 표현식으로 정렬한다") {
        queryOf("findSimilarPostsExcluding") shouldContain indexExpression
    }
})
