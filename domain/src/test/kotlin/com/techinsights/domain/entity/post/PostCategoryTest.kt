package com.techinsights.domain.entity.post

import com.techinsights.domain.entity.company.Company
import com.techinsights.domain.enums.Category
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PostCategoryTest : FunSpec({

  fun createTestPost(): Post {
    val company = Company(
      id = 1L,
      name = "TestCompany",
      blogUrl = "https://test.com",
      logoImageName = "logo.png"
    )
    return Post(
      id = 1L,
      title = "Test Post",
      preview = "Preview",
      url = "https://test.com/post1",
      content = "Content",
      publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
      company = company
    )
  }

  test("categoryValues should return empty set when postCategories is empty") {
    val post = createTestPost()

    post.categoryValues.shouldBeEmpty()
  }

  test("categoryValues should extract categories from postCategories") {
    val post = createTestPost()
    post.postCategories.add(PostCategory(id = 1L, post = post, category = Category.AI))
    post.postCategories.add(PostCategory(id = 2L, post = post, category = Category.BackEnd))

    post.categoryValues shouldBe setOf(Category.AI, Category.BackEnd)
  }

  test("updateCategories should add new categories") {
    val post = createTestPost()

    post.updateCategories(setOf(Category.AI, Category.FrontEnd))

    post.postCategories shouldHaveSize 2
    post.categoryValues shouldBe setOf(Category.AI, Category.FrontEnd)
  }

  test("updateCategories should remove categories not in the new set") {
    val post = createTestPost()
    post.postCategories.add(PostCategory(id = 1L, post = post, category = Category.AI))
    post.postCategories.add(PostCategory(id = 2L, post = post, category = Category.BackEnd))

    post.updateCategories(setOf(Category.AI))

    post.postCategories shouldHaveSize 1
    post.categoryValues shouldBe setOf(Category.AI)
  }

  test("updateCategories should handle replacing all categories") {
    val post = createTestPost()
    post.postCategories.add(PostCategory(id = 1L, post = post, category = Category.AI))

    post.updateCategories(setOf(Category.FrontEnd, Category.Infra))

    post.postCategories shouldHaveSize 2
    post.categoryValues shouldBe setOf(Category.FrontEnd, Category.Infra)
  }

  test("updateCategories with empty set should clear all categories") {
    val post = createTestPost()
    post.postCategories.add(PostCategory(id = 1L, post = post, category = Category.AI))

    post.updateCategories(emptySet())

    post.postCategories.shouldBeEmpty()
  }

  test("updateCategories with same categories should not change the set") {
    val post = createTestPost()
    post.postCategories.add(PostCategory(id = 1L, post = post, category = Category.AI))
    post.postCategories.add(PostCategory(id = 2L, post = post, category = Category.BackEnd))

    post.updateCategories(setOf(Category.AI, Category.BackEnd))

    post.postCategories shouldHaveSize 2
    post.categoryValues shouldBe setOf(Category.AI, Category.BackEnd)
  }
})
