package com.techinsights.batch.config

import com.techinsights.batch.processor.PostEmbeddingProcessor
import com.techinsights.batch.processor.PostSummaryProcessor
import com.techinsights.batch.reader.PostReader
import com.techinsights.batch.writer.PostWriter
import com.techinsights.domain.dto.post.PostDto
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.support.CompositeItemProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class SummarizePostJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val reader: PostReader,
    private val postSummaryProcessor: PostSummaryProcessor,
    private val postEmbeddingProcessor: PostEmbeddingProcessor,
    private val writer: PostWriter
) {

    @Bean
    fun summarizePostJob(@Qualifier("summarizePostStep") summarizePostStep: Step): Job =
        JobBuilder("summarizePostJob", jobRepository)
            .incrementer(RunIdIncrementer())
            .start(summarizePostStep)
            .build()

    @Bean
    fun summarizePostStep(): Step =
        StepBuilder("summarizePostStep", jobRepository)
            .chunk<List<PostDto>, List<PostDto>>(1, transactionManager)
            .reader(chunkListItemReader(reader))
            .processor(compositeProcessor())
            .writer(writer)
            .faultTolerant()
            .retryLimit(3).retry(Exception::class.java)
            .skipLimit(10).skip(Exception::class.java)
            .build()

    @Bean
    fun compositeProcessor(): ItemProcessor<List<PostDto>, List<PostDto>> {
        return CompositeItemProcessor<List<PostDto>, List<PostDto>>().apply {
            setDelegates(listOf(postSummaryProcessor, postEmbeddingProcessor))
        }
    }

    @Bean
    @StepScope
    fun chunkListItemReader(postReader: PostReader): ItemReader<List<PostDto>> {
        return object : ItemReader<List<PostDto>> {
            private var finished = false

            override fun read(): List<PostDto>? {
                if (finished) {
                    return null
                }

                val items = mutableListOf<PostDto>()
                for (i in 0 until CHUNK_SIZE) {
                    val item = postReader.read() ?: break
                    items.add(item)
                }

                if (items.isEmpty()) {
                    finished = true
                    return null
                }

                if (items.size < CHUNK_SIZE) {
                    finished = true
                }

                return items
            }
        }
    }

    companion object {
        private const val CHUNK_SIZE = 100
    }
}