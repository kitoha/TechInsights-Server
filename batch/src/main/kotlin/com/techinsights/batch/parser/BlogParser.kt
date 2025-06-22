package com.techinsights.batch.parser

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto

interface BlogParser {
  fun supports(feedUrl: String): Boolean
  suspend fun parseList(companyDto: CompanyDto, content: String): List<PostDto>
}