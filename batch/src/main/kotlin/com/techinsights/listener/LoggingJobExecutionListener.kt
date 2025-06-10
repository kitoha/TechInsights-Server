package com.techinsights.listener

import org.slf4j.LoggerFactory
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component

@Component
class LoggingJobExecutionListener : JobExecutionListener {
  private val log = LoggerFactory.getLogger(LoggingJobExecutionListener::class.java)

  override fun beforeJob(jobExecution: JobExecution) {
    log.info("====[Batch] {} 시작 | JobParameters: {}====", jobExecution.jobInstance.jobName, jobExecution.jobParameters)
  }

  override fun afterJob(jobExecution: JobExecution) {
    val stepSummaries = jobExecution.stepExecutions.map { step ->
      """
            Step: ${step.stepName}
                READ  : ${step.readCount}
                WRITE : ${step.writeCount}
                SKIP  : ${step.skipCount}
                FILTER: ${step.filterCount}
                ERROR : ${step.rollbackCount}
                EXIT  : ${step.exitStatus.exitCode}
            """.trimIndent()
    }.joinToString("\n")

    log.info(
      """
            ==== [Batch] ${jobExecution.jobInstance.jobName} 종료 ====
            - ExitStatus: ${jobExecution.exitStatus.exitCode}
            - Status    : ${jobExecution.status}
            - 처리 요약 :
            $stepSummaries
            """.trimIndent()
    )

    if (jobExecution.allFailureExceptions.isNotEmpty()) {
      log.error("Batch 실패 예외 목록: ${jobExecution.allFailureExceptions.joinToString("\n") { it.message ?: "" }}")
    }
  }
}