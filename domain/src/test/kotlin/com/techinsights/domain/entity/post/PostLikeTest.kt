package com.techinsights.domain.entity.post

import com.techinsights.domain.utils.Tsid
import com.techinsights.domain.utils.decode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PostLikeTest : FunSpec({

    test("PostLike created with userId (Authenticated User) should hold correct values") {
        val postId = Tsid.generate().decode()
        val userId = 12345L
        val ipAddress = "127.0.0.1"

        val postLike = PostLike(
            id = Tsid.generate().decode(),
            postId = postId,
            userId = userId,
            ipAddress = ipAddress // IP is always captured
        )

        postLike.postId shouldBe postId
        postLike.userId shouldBe userId
        postLike.ipAddress shouldBe ipAddress
    }

    test("PostLike created without userId (Anonymous User) should have null userId") {
        val postId = Tsid.generate().decode()
        val ipAddress = "127.0.0.1"

        val postLike = PostLike(
            id = Tsid.generate().decode(),
            postId = postId,
            userId = null,
            ipAddress = ipAddress
        )

        postLike.userId shouldBe null
        postLike.ipAddress shouldBe ipAddress
    }
})
