package com.techinsights.batch.github.readme.config

import com.techinsights.batch.common.listener.LoggingJobExecutionListener
import com.techinsights.batch.github.readme.config.props.GithubReadmeBatchProperties
import com.techinsights.batch.github.readme.processor.GithubReadmeFetchProcessor
import com.techinsights.batch.github.readme.reader.UnsummarizedRepoReader
import com.techinsights.batch.github.readme.writer.GithubReadmeBatchSummaryWriter
import com.techinsights.domain.dto.gemini.ArticleInput
import com.techinsights.domain.dto.github.GithubRepositoryDto
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@EnableConfigurationProperties(GithubReadmeBatchProperties::class)
class GithubReadmeSummaryJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val properties: GithubReadmeBatchProperties,
    private val unsummarizedRepoReader: UnsummarizedRepoReader,
    private val githubReadmeFetchProcessor: GithubReadmeFetchProcessor,
    private val githubReadmeBatchSummaryWriter: GithubReadmeBatchSummaryWriter,
    private val loggingJobExecutionListener: LoggingJobExecutionListener,
) {
    @Bean
    fun githubReadmeSummaryJob(
        @Qualifier("githubReadmeSummaryStep") step: Step,
    ): Job = JobBuilder(properties.jobName, jobRepository)
        .incrementer(RunIdIncrementer())
        .listener(loggingJobExecutionListener)
        .start(step)
        .build()

    @Bean("githubReadmeSummaryStep")
    fun githubReadmeSummaryStep(): Step =
        StepBuilder(properties.stepName, jobRepository)
            .chunk<GithubRepositoryDto, ArticleInput>(properties.chunkSize, transactionManager)
            .reader(unsummarizedRepoReader)
            .processor(githubReadmeFetchProcessor)
            .writer(githubReadmeBatchSummaryWriter)
            .build()
}
