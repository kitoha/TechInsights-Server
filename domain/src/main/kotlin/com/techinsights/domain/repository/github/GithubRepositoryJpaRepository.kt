package com.techinsights.domain.repository.github

import com.techinsights.domain.entity.github.GithubRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GithubRepositoryJpaRepository : JpaRepository<GithubRepository, Long> {

    @Query(
        value = """
        SELECT full_name         AS fullName,
               repo_name         AS repoName,
               description,
               readme_summary    AS readmeSummary,
               primary_language  AS primaryLanguage,
               star_count        AS starCount,
               owner_name        AS ownerName,
               owner_avatar_url  AS ownerAvatarUrl,
               topics,
               html_url          AS htmlUrl,
               readme_embedding_vector <-> CAST(:targetVector AS vector) AS distance
        FROM github_repositories
        WHERE deleted_at IS NULL
          AND readme_embedding_vector IS NOT NULL
        ORDER BY distance
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findSimilarRepositories(targetVector: String, limit: Long): List<GithubRepositoryWithDistance>
}
