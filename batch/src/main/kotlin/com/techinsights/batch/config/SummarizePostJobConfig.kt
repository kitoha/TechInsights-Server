package com.techinsights.batch.config

import com.techinsights.batch.listener.LoggingJobExecutionListener
import com.techinsights.batch.listener.LoggingSkipListener
import com.techinsights.batch.processor.PostSummaryProcessor
import com.techinsights.batch.reader.SummarizedPostReader
import com.techinsights.batch.writer.PostWriter
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
class SummarizePostJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val reader: SummarizedPostReader,
    private val postSummaryProcessor: PostSummaryProcessor,
    private val writer: PostWriter,
    private val loggingJobExecutionListener: LoggingJobExecutionListener,
    private val loggingSkipListener: LoggingSkipListener
) {

    @Bean
    fun summarizePostJob(@Qualifier("summarizePostStep") summarizePostStep: Step): Job =
        JobBuilder("summarizePostJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .listener(loggingJobExecutionListener)
            .start(summarizePostStep)
            .build()

    @Bean
    fun summarizePostStep(): Step =
        StepBuilder("summarizePostStep", jobRepository)
            .chunk<PostDto, PostDto>(CHUNK_SIZE, transactionManager)
            .reader(reader)
            .processor(postSummaryProcessor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(1000) // 스킵 제한을 넉넉하게 설정
            .skip(Exception::class.java)
            .listener(loggingSkipListener)
            .build()

    companion object {
        private const val CHUNK_SIZE = 100
    }
}