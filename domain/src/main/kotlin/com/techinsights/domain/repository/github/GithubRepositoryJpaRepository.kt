package com.techinsights.domain.repository.github

import com.techinsights.domain.entity.github.GithubRepository
import org.springframework.data.jpa.repository.JpaRepository

interface GithubRepositoryJpaRepository : JpaRepository<GithubRepository, Long>
