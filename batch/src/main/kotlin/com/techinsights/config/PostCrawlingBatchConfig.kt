package com.techinsights.config

import com.techinsights.dto.post.PostDto
import com.techinsights.entity.company.Company
import com.techinsights.processor.RawPostProcessor
import com.techinsights.reader.CompanyReader
import com.techinsights.writer.RawPostWriter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class PostCrawlingBatchConfig (
  private val jobRepository: JobRepository,
  private val transactionManager: PlatformTransactionManager,
  private val companyReader: CompanyReader,
  private val rawPostProcessor: RawPostProcessor,
  private val rawPostWriter: RawPostWriter,
  private val properties: PostCrawlingBatchProperties
){

  @Bean
  fun crawlPostJob(): Job = JobBuilder(properties.jobName, jobRepository)
    .start(crawlPostStep())
    .build()

  @Bean
  fun crawlPostStep(): Step = StepBuilder(properties.stepName, jobRepository)
    .chunk<Company, List<PostDto>>(properties.chunkSize, transactionManager)
    .reader(companyReader)
    .processor(rawPostProcessor)
    .writer(rawPostWriter)
    .faultTolerant()
    .retryLimit(properties.retryLimit)
    .retry(Exception::class.java)
    .build()
}