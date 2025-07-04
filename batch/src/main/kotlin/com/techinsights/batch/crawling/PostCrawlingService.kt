package com.techinsights.batch.crawling

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto

interface PostCrawlingService {
  suspend fun processCrawledData(companyDto: CompanyDto): List<PostDto>
}