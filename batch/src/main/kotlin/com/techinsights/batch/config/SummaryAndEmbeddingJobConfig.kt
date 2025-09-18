package com.techinsights.batch.config

import com.techinsights.batch.listener.LoggingJobExecutionListener
import com.techinsights.batch.listener.LoggingSkipListener
import com.techinsights.batch.processor.PostEmbeddingProcessor
import com.techinsights.batch.processor.PostSummaryProcessor
import com.techinsights.batch.reader.PostReader
import com.techinsights.batch.reader.SummarizedPostReader
import com.techinsights.batch.writer.PostEmbeddingWriter
import com.techinsights.batch.writer.PostWriter
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.dto.post.PostDto
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class SummaryAndEmbeddingJobConfig(
  private val jobRepository: JobRepository,
  private val transactionManager: PlatformTransactionManager,
  private val postSummaryProcessor: PostSummaryProcessor,
  private val postWriter: PostWriter,
  private val postEmbeddingProcessor: PostEmbeddingProcessor,
  private val postEmbeddingWriter: PostEmbeddingWriter,
  private val loggingJobExecutionListener: LoggingJobExecutionListener,
  private val loggingSkipListener: LoggingSkipListener,
  private val summarizePostReader: PostReader,
  private val embeddingPostReader: SummarizedPostReader,
) {

  @Bean
  fun summaryAndEmbeddingJob(
    @Qualifier("summarizePostStep") summarizePostStep: Step,
    @Qualifier("postVectorEmbeddingStep") postVectorEmbeddingStep: Step
  ): Job =
    JobBuilder("summaryAndEmbeddingJob", jobRepository)
      .incrementer(RunIdIncrementer())
      .listener(loggingJobExecutionListener)
      .start(summarizePostStep)
      .next(postVectorEmbeddingStep)
      .build()

  @Bean
  fun summarizePostStep(): Step =
    StepBuilder("summarizePostStep", jobRepository)
      .chunk<PostDto, PostDto>(CHUNK_SIZE, transactionManager)
      .reader(summarizePostReader)
      .processor(postSummaryProcessor)
      .writer(postWriter)
      .faultTolerant()
      .skipLimit(10)
      .skip(Exception::class.java)
      .listener(loggingSkipListener)
      .build()

  @Bean
  fun postVectorEmbeddingStep(): Step =
    StepBuilder("postVectorEmbeddingStep", jobRepository)
      .chunk<PostDto, PostEmbeddingDto?>(EMBEDDING_CHUNK_SIZE, transactionManager)
      .reader(embeddingPostReader)
      .processor(postEmbeddingProcessor)
      .writer(postEmbeddingWriter)
      .faultTolerant()
      .skipLimit(10)
      .skip(Exception::class.java)
      .build()


  companion object {

    private const val CHUNK_SIZE = 10
    private const val EMBEDDING_CHUNK_SIZE = 10
  }
}
