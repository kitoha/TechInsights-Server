package com.techinsights.api

import com.techinsights.domain.dto.embedding.EmbeddingRequest
import com.techinsights.domain.enums.GeminiModelType
import com.techinsights.domain.service.embedding.EmbeddingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
  private val embeddingService: EmbeddingService
) {

  /**
   * vector embedding test endpoint.
   * This endpoint is used to test the generation of vector embeddings using the Gemini model.
   * after test is end you can delete this endpoint.
   * @param input EmbeddingRequest containing the text to be embedded.
   * @return List of floats representing the generated embedding.
   */
  // Example endpoint to test embedding generation
   @GetMapping("/test/embedding")
   fun testEmbedding(@RequestBody input: EmbeddingRequest): List<Float> {
       return embeddingService.generateEmbedding(input, GeminiModelType.GEMINI_EMBEDDING)
   }

}