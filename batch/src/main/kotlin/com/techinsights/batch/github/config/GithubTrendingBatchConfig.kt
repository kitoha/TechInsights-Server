package com.techinsights.batch.github.config

import com.techinsights.batch.common.listener.LoggingJobExecutionListener
import com.techinsights.batch.github.config.props.GithubBatchProperties
import com.techinsights.batch.github.dto.GithubRepoUpsertData
import com.techinsights.batch.github.dto.GithubSearchResponse
import com.techinsights.batch.github.processor.GithubRepoProcessor
import com.techinsights.batch.github.reader.GithubRepoReader
import com.techinsights.batch.github.writer.GithubRepoWriter
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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@EnableConfigurationProperties(GithubBatchProperties::class)
class GithubTrendingBatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val githubRepoReader: GithubRepoReader,
    private val githubRepoProcessor: GithubRepoProcessor,
    private val githubRepoWriter: GithubRepoWriter,
    private val properties: GithubBatchProperties,
    private val loggingJobExecutionListener: LoggingJobExecutionListener,
) {
    @Bean
    fun githubTrendingJob(
        @Qualifier("githubTrendingStep") step: Step,
    ): Job = JobBuilder(properties.jobName, jobRepository)
        .incrementer(RunIdIncrementer())
        .listener(loggingJobExecutionListener)
        .start(step)
        .build()

    /**
     * 단일 스레드 Step — Reader 내부에서 supervisorScope로 병렬 fetch 후
     * 버퍼에서 순차 소비하므로 @Synchronized / race condition 불필요.
     */
    @Bean
    fun githubTrendingStep(): Step =
        StepBuilder(properties.stepName, jobRepository)
            .chunk<GithubSearchResponse.Item, GithubRepoUpsertData>(
                properties.chunkSize,
                transactionManager,
            )
            .reader(githubRepoReader)
            .processor(githubRepoProcessor)
            .writer(githubRepoWriter)
            .faultTolerant()
            .skip(DataIntegrityViolationException::class.java)
            .skipLimit(10)
            .build()
}
