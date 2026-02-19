package com.techinsights.batch.embedding.config

import com.techinsights.batch.embedding.processor.BatchPostEmbeddingProcessor
import com.techinsights.batch.embedding.reader.BatchSummarizedPostReader
import com.techinsights.batch.embedding.writer.BatchPostEmbeddingWriter
import com.techinsights.domain.dto.embedding.PostEmbeddingDto
import com.techinsights.domain.dto.post.PostDto
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class PostVectorEmbeddingConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val batchSummarizedPostReader: BatchSummarizedPostReader,
    private val batchPostEmbeddingProcessor: BatchPostEmbeddingProcessor,
    private val batchPostEmbeddingWriter: BatchPostEmbeddingWriter
) {

    @Bean
    fun postVectorEmbeddingJob(): Job =
        JobBuilder("postVectorEmbeddingJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(postVectorEmbeddingStep())
            .build()

    @Bean("postVector")
    fun postVectorEmbeddingStep(): Step =
        StepBuilder("postVectorEmbeddingStep", jobRepository)
            .chunk<List<PostDto>, List<PostEmbeddingDto>>(CHUNK_SIZE, transactionManager)
            .reader(batchSummarizedPostReader)
            .processor(batchPostEmbeddingProcessor)
            .writer(batchPostEmbeddingWriter)
            .faultTolerant()
            .retryLimit(3).retry(Exception::class.java)
            .skipLimit(10).skip(Exception::class.java)
            .build()

    companion object {
        private const val CHUNK_SIZE = 1
    }
}
