package com.techinsights.batch.config

import com.techinsights.domain.dto.company.CompanyDto
import com.techinsights.domain.dto.post.PostDto
import com.techinsights.batch.listener.LoggingJobExecutionListener
import com.techinsights.batch.processor.RawPostProcessor
import com.techinsights.batch.reader.CompanyReader
import com.techinsights.batch.writer.RawPostWriter
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
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
  private val properties: PostCrawlingBatchProperties,
  private val loggingJobExecutionListener: LoggingJobExecutionListener
){

  @Bean
  fun crawlPostJob(@Qualifier("crawlPostStep") crawlPostStep: Step): Job = JobBuilder(properties.jobName, jobRepository)
    .listener(loggingJobExecutionListener)
    .start(crawlPostStep)
    .build()

  @Bean
  fun crawlPostStep(): Step = StepBuilder(properties.stepName, jobRepository)
    .chunk<CompanyDto, List<PostDto>>(properties.chunkSize, transactionManager)
    .reader(companyReader)
    .processor(rawPostProcessor)
    .writer(rawPostWriter)
    .faultTolerant()
    .retry(Exception::class.java)
    .retryLimit(properties.retryLimit)
    .skip(Exception::class.java)
    .skipLimit(10)
    .build()
}