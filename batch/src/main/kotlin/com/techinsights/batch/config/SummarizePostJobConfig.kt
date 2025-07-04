package com.techinsights.batch.config

import com.techinsights.batch.processor.PostSummaryProcessor
import com.techinsights.batch.reader.PostReader
import com.techinsights.batch.writer.PostWriter
import com.techinsights.domain.dto.post.PostDto
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
class SummarizePostJobConfig (
  private val jobRepository: JobRepository,
  private val transactionManager: PlatformTransactionManager,
  private val reader: PostReader,
  private val processor: PostSummaryProcessor,
  private val writer: PostWriter
){

  @Bean
  fun summarizePostJob(@Qualifier("summarizePostStep") summarizePostStep: Step): Job = JobBuilder("summarizePostJob", jobRepository)
      .start(summarizePostStep)
      .build()

  @Bean
  fun summarizePostStep(): Step =
    StepBuilder("summarizePostStep", jobRepository)
      .chunk<PostDto, PostDto>(CHUNK_SIZE, transactionManager)
      .reader(reader)
      .processor(processor)
      .writer(writer)
      .faultTolerant()
      .retryLimit(3).retry(Exception::class.java)
      .skipLimit(10).skip(Exception::class.java)
      .build()

  companion object { private const val CHUNK_SIZE = 100 }

}