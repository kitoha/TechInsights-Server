package com.techinsights.domain.entity.post

import com.techinsights.domain.enums.Category
import jakarta.persistence.*

@Entity
@Table(
  name = "post_categories",
  uniqueConstraints = [
    UniqueConstraint(
      name = "uk_post_categories_post_category",
      columnNames = ["post_id", "category"]
    )
  ]
)
class PostCategory(
  @Id
  val id: Long,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  val post: Post,

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  val category: Category
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PostCategory) return false
    return post.id == other.post.id && category == other.category
  }

  override fun hashCode(): Int {
    var result = post.id.hashCode()
    result = 31 * result + category.hashCode()
    return result
  }
}
