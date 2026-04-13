package com.techinsights.batch.github.community.analyze.config

import com.techinsights.batch.common.listener.LoggingJobExecutionListener
import com.techinsights.batch.github.community.analyze.config.props.CommunityAnalyzeBatchProperties
import com.techinsights.batch.github.community.analyze.processor.CommunityAnalyzeProcessor
import com.techinsights.batch.github.community.analyze.reader.CommunityAnalyzeReader
import com.techinsights.batch.github.community.analyze.writer.CommunityAnalyzeWriter
import com.techinsights.domain.dto.community.CommunityAnalysisInput
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
@EnableConfigurationProperties(CommunityAnalyzeBatchProperties::class)
class CommunityAnalyzeJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val properties: CommunityAnalyzeBatchProperties,
    private val communityAnalyzeReader: CommunityAnalyzeReader,
    private val communityAnalyzeProcessor: CommunityAnalyzeProcessor,
    private val communityAnalyzeWriter: CommunityAnalyzeWriter,
    private val loggingJobExecutionListener: LoggingJobExecutionListener,
) {

    @Bean
    fun communityAnalyzeJob(
        @Qualifier("communityAnalyzeStep") step: Step,
    ): Job = JobBuilder(properties.jobName, jobRepository)
        .incrementer(RunIdIncrementer())
        .listener(loggingJobExecutionListener)
        .start(step)
        .build()

    @Bean("communityAnalyzeStep")
    fun communityAnalyzeStep(): Step =
        StepBuilder(properties.stepName, jobRepository)
            .chunk<GithubRepositoryDto, CommunityAnalysisInput>(properties.chunkSize, transactionManager)
            .reader(communityAnalyzeReader)
            .processor(communityAnalyzeProcessor)
            .writer(communityAnalyzeWriter)
            .build()
}
