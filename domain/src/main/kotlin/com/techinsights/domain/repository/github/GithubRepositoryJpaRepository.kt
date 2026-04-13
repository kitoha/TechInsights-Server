package com.techinsights.domain.repository.github

import com.techinsights.domain.entity.github.GithubRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GithubRepositoryJpaRepository : JpaRepository<GithubRepository, Long> {

    @Query(
        value = """
        SELECT gr.full_name        AS fullName,
               gr.repo_name        AS repoName,
               gr.description,
               grr.readme_summary  AS readmeSummary,
               gr.primary_language AS primaryLanguage,
               gr.star_count       AS starCount,
               gr.owner_name       AS ownerName,
               gr.owner_avatar_url AS ownerAvatarUrl,
               gr.topics,
               gr.html_url         AS htmlUrl,
               grr.readme_embedding_vector::halfvec(3072) <=> CAST(:targetVector AS halfvec) AS distance
        FROM github_repositories gr
        JOIN github_repository_readme grr ON grr.repo_id = gr.id
        WHERE gr.deleted_at IS NULL
          AND grr.readme_embedding_vector IS NOT NULL
        ORDER BY distance
        LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findSimilarRepositories(targetVector: String, limit: Long): List<GithubRepositoryWithDistance>
}
