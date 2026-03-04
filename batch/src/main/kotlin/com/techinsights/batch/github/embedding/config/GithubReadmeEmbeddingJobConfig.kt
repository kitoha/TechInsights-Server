package com.techinsights.batch.github.embedding.config

import com.techinsights.batch.common.listener.LoggingJobExecutionListener
import com.techinsights.batch.github.embedding.config.props.GithubEmbeddingBatchProperties
import com.techinsights.batch.github.embedding.dto.GithubEmbeddingRequestDto
import com.techinsights.batch.github.embedding.processor.GithubReadmeEmbeddingProcessor
import com.techinsights.batch.github.embedding.reader.UnembeddedRepoReader
import com.techinsights.batch.github.embedding.writer.GithubReadmeEmbeddingWriter
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
@EnableConfigurationProperties(GithubEmbeddingBatchProperties::class)
class GithubReadmeEmbeddingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val properties: GithubEmbeddingBatchProperties,
    private val unembeddedRepoReader: UnembeddedRepoReader,
    private val githubReadmeEmbeddingProcessor: GithubReadmeEmbeddingProcessor,
    private val githubReadmeEmbeddingWriter: GithubReadmeEmbeddingWriter,
    private val loggingJobExecutionListener: LoggingJobExecutionListener,
) {
    @Bean
    fun githubReadmeEmbeddingJob(
        @Qualifier("githubReadmeEmbeddingStep") step: Step,
    ): Job = JobBuilder(properties.jobName, jobRepository)
        .incrementer(RunIdIncrementer())
        .listener(loggingJobExecutionListener)
        .start(step)
        .build()

    @Bean("githubReadmeEmbeddingStep")
    fun githubReadmeEmbeddingStep(): Step =
        StepBuilder(properties.stepName, jobRepository)
            .chunk<GithubRepositoryDto, GithubEmbeddingRequestDto>(properties.chunkSize, transactionManager)
            .reader(unembeddedRepoReader)
            .processor(githubReadmeEmbeddingProcessor)
            .writer(githubReadmeEmbeddingWriter)
            .faultTolerant()
            .retryLimit(3).retry(Exception::class.java)
            .skipLimit(10).skip(Exception::class.java)
            .build()
}
