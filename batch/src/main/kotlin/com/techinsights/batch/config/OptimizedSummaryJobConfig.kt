package com.techinsights.batch.config

import com.techinsights.batch.listener.LoggingJobExecutionListener
import com.techinsights.batch.processor.AsyncBatchPostSummaryProcessor
import com.techinsights.batch.reader.BatchAwarePostReader
import com.techinsights.batch.writer.FlatteningPostWriter
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
class OptimizedSummaryJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val batchAwarePostReader: BatchAwarePostReader,
    private val asyncBatchProcessor: AsyncBatchPostSummaryProcessor,
    private val flatteningPostWriter: FlatteningPostWriter,
    private val loggingJobExecutionListener: LoggingJobExecutionListener
) {

    @Bean
    fun optimizedSummaryJob(
        @Qualifier("optimizedSummaryStep") step: Step
    ): Job = JobBuilder("optimizedSummaryJob", jobRepository)
        .incrementer(RunIdIncrementer())
        .listener(loggingJobExecutionListener)
        .start(step)
        .build()

    @Bean
    fun optimizedSummaryStep(): Step =
        StepBuilder("optimizedSummaryStep", jobRepository)
            .chunk<List<PostDto>, List<PostDto>>(CHUNK_SIZE, transactionManager)
            .reader(batchAwarePostReader)
            .processor(asyncBatchProcessor)
            .writer(flatteningPostWriter)
            .faultTolerant()
            .skipLimit(20)
            .skip(Exception::class.java)
            .build()

    companion object {
        /**
         * 현재 사용 중인 Gemini API의 Rate Limit이 분당 5회(5 RPM)로 매우 제한적입니다.
         * 추후 API Tier 업그레이드 시 이 값을 5~10 이상으로 상향 조정하여 처리 속도를 높일 수 있습니다.
         */
        private const val CHUNK_SIZE = 1
    }
}
