# Technical Challenges & Solutions

이 문서에서는 Tech Insights 백엔드 개발 과정에서 마주한 기술적 도전과 해결 방안을 상세히 설명합니다.

---

## 목차

- [1. 배치 처리 아키텍처 고도화](#1-배치-처리-아키텍처-고도화)
- [2. N+1 쿼리 최적화](#2-n1-쿼리-최적화)
- [3. 벡터 검색 성능 최적화](#3-벡터-검색-성능-최적화)
- [4. 조회수 집계 트랜잭션 최적화](#4-조회수-집계-트랜잭션-최적화)
- [5. Gemini API Rate Limit 관리](#5-gemini-api-rate-limit-관리)
- [6. 스트리밍 기반 타임아웃 방지](#6-스트리밍-기반-타임아웃-방지)
- [7. RSS/Atom 피드 파싱 및 중복 처리](#7-rssatom-피드-파싱-및-중복-처리)
- [8. 배치 요약 검증](#8-배치-요약-검증)
- [9. 요약 실패 관리 및 재시도](#9-요약-실패-관리-및-재시도)

---

## 1. 배치 처리 아키텍처 고도화

### 문제 상황

Gemini API를 사용한 대량 게시글 요약 처리 시 다음과 같은 문제가 발생했습니다:
- 단건 요약 방식으로는 일일 처리량 제한 (초기 20건/일)
- API Rate Limit로 인한 잦은 타임아웃 (12% 발생률)
- 실패 시 전체 재처리로 인한 비효율
- 대용량 응답으로 인한 메모리 부담

### 해결 방안

#### 1. DynamicBatchBuilder: 토큰 기반 동적 배치 분할

Gemini API의 입력 토큰 제한(200K)과 출력 토큰 제한(14.7K)을 고려하여 배치를 동적으로 분할합니다.

```kotlin
@Component
class DynamicBatchBuilder(
    private val config: BatchBuildConfig,
    private val limitChecker: BatchLimitChecker,
    private val postTruncator: PostTruncator
) {
    fun buildBatches(posts: List<PostDto>): List<Batch> {
        val batches = mutableListOf<Batch>()
        var currentBatch = mutableListOf<PostDto>()
        var currentTokens = config.basePromptTokens

        for (post in posts) {
            val postTokens = TokenEstimator.estimateTotalTokens(post.content)

            // 단일 게시글이 최대 토큰 초과 시 자동 truncation
            if (limitChecker.exceedsMaxTokens(postTokens)) {
                handleOversizedPost(post, batches, currentBatch, currentTokens)
                currentBatch = mutableListOf()
                currentTokens = config.basePromptTokens
                continue
            }

            // 입력/출력 토큰 제한 또는 배치 크기 초과 시 새 배치 시작
            if (shouldStartNewBatch(currentBatch.size, currentTokens, postTokens)) {
                finalizeBatch(batches, currentBatch, currentTokens)
                currentBatch = mutableListOf()
                currentTokens = config.basePromptTokens
            }

            currentBatch.add(post)
            currentTokens += postTokens
        }

        finalizeBatch(batches, currentBatch, currentTokens)
        return batches
    }

    private fun shouldStartNewBatch(
        currentSize: Int,
        currentTokens: Int,
        additionalTokens: Int
    ): Boolean {
        if (currentSize == 0) return false

        val exceedsInput = limitChecker.exceedsInputLimit(currentTokens, additionalTokens)
        val exceedsOutput = limitChecker.exceedsOutputLimit(currentSize + 1)
        val exceedsSize = limitChecker.exceedsBatchSize(currentSize)

        return exceedsInput || exceedsOutput || exceedsSize
    }
}
```

#### 2. generateContentStream: 스트리밍 API로 타임아웃 방지

Gemini의 스트리밍 API를 사용하여 청크 단위로 응답을 수신함으로써 타임아웃을 방지합니다.

```kotlin
@Service
class GeminiBatchArticleSummarizer(
    private val geminiClient: Client,
    rateLimiterRegistry: RateLimiterRegistry,
    circuitBreakerRegistry: CircuitBreakerRegistry
) {
    private val rpmLimiter = rateLimiterRegistry.rateLimiter("geminiBatchRpm")
    private val rpdLimiter = rateLimiterRegistry.rateLimiter("geminiBatchRpd")
    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker("geminiBatch")

    override fun summarizeBatch(
        articles: List<ArticleInput>,
        modelType: GeminiModelType
    ): Flow<SummaryResultWithId> = channelFlow {
        // Rate Limiter 획득
        acquireRateLimiterPermission(rpdLimiter)
        acquireRateLimiterPermission(rpmLimiter)

        // Circuit Breaker로 보호된 스트리밍 API 호출
        val responseStream = circuitBreaker.executeCallable {
            geminiClient.models.generateContentStream(modelName, prompt, config)
        }

        val jsonParser = StreamingJsonParser()
        for (res in responseStream) {
            res.text()?.let { textChunk ->
                // 청크 단위로 JSON 파싱 및 즉시 처리
                jsonParser.process(textChunk).forEach { summary ->
                    launch(ioDispatcher) { send(summary) }
                }
            }
        }
    }
}
```

#### 3. 부분 성공 처리 (Partial Success)

배치 내 일부 게시글 요약이 실패해도 성공한 게시글은 정상 처리하고, 실패 게시글만 재시도 큐에 등록합니다.

```kotlin
override fun summarizeBatch(
    articles: List<ArticleInput>,
    modelType: GeminiModelType
): Flow<SummaryResultWithId> {
    return channelFlow {
        val results = mutableSetOf<String>()

        callGeminiApi(articles, modelType).collect { result ->
            results.add(result.id)
            send(result)  // 성공/실패 여부와 관계없이 결과 전송
        }

        // 응답에 누락된 게시글 처리
        articles.filter { it.id !in results }.forEach { missing ->
            send(
                SummaryResultWithId(
                    id = missing.id,
                    success = false,
                    error = "Response missing from Gemini stream",
                    errorType = ErrorType.CONTENT_ERROR
                )
            )
        }
    }
}
```

#### 4. 성능 측정 결과 (2025.12.27 - 2026.01.15)

| 지표 | 개선 전 | 개선 후 | 향상률 |
|------|---------|---------|--------|
| **일일 처리량** | 20건 | 140건 | **7배** |
| **API 호출 효율** | 20회/20건 | 4회/20건 | **5배** |
| **타임아웃 발생률** | 12% (6/50) | 2% (1/50) | **83% 감소** |
| **평균 처리 시간** | 13.71분/36건 | 2.11분/36건 | **84.6% 단축** |

### 핵심 포인트

- **토큰 기반 동적 배치**: 입력/출력 토큰 제한을 고려한 지능적 배치 분할
- **스트리밍 API**: 청크 단위 수신으로 타임아웃 방지 (타임아웃률 12% → 2%)
- **부분 성공 처리**: 배치 내 개별 실패 격리로 재처리 효율 극대화
- **Rate Limiting**: Resilience4j로 RPM/RPD 제한 준수
- **Circuit Breaker**: 연속 실패 시 빠른 실패 반환으로 시스템 보호

---

## 2. N+1 쿼리 최적화

### 문제 상황

기존 `recordView()` 메서드에서 중복 체크, PostView 저장, Post 조회수 증가, Company 총 조회수 증가가 단일 트랜잭션으로 묶여 있었습니다. 트랜잭션 범위가 과도하게 넓어지면서 DB Lock 경합이 발생하고, 조회수 업데이트 실패 시 전체 조회 기록이 롤백되는 문제가 있었습니다.

### 해결 방안

Spring Event와 `@TransactionalEventListener`를 활용하여 관심사를 분리했습니다.
(Redis를 현재 사용하지 못 하기 때문에 임시로 DB 기반 비동기 처리를 구현)

1. 조회 기록 저장: 중복 체크 및 PostView 저장은 기존 트랜잭션 내에서 처리
2. 조회수 증가: Post 및 Company 조회수 증가는 별도의 비동기 이벤트
3. 트랜잭션 커밋 후 실행: 조회 기록이 성공적으로 커밋된 후에만 조회수 증가 로직 실행
4. 장애 격리: 조회수 증가 실패 시에도 조회 기록은 유지
5. 비동기 처리: 사용자 응답 지연 방지
6. Eventual Consistency: 조회수는 최종적으로 일관성을 보장
7. 성능 향상: 트랜잭션 경합 감소로 전체 처리량 증가

```kotlin
@Component
class ViewCountEventHandler(
  private val viewCountUpdater: ViewCountUpdater,
  private val companyViewCountUpdater: CompanyViewCountUpdater
) {
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleViewCountIncrement(event: ViewCountIncrementEvent) {
    try {
      viewCountUpdater.incrementViewCount(event.postId)
      companyViewCountUpdater.incrementTotalViewCount(event.companyId)
    } catch (e: Exception) {
      logger.error(e) { "Failed to increment view count" }
    }
  }
}
```

### 핵심 포인트

- **AFTER_COMMIT**: 조회 기록 트랜잭션이 성공적으로 커밋된 후에만 카운트 증가 로직 실행
- **@Async**: 비동기 처리로 사용자 응답 지연 방지
- **Eventual Consistency**: 카운트 실패가 핵심 비즈니스 로직에 영향을 주지 않음

### Querydsl fetchJoin 전략

Post-Company 관계에서 발생하는 N+1 문제를 Querydsl의 fetchJoin으로 해결했습니다.

```kotlin
val posts = queryFactory.selectFrom(postEntity)
  .leftJoin(postEntity.company, companyEntity).fetchJoin()
  .where(postEntity.url.`in`(urls))
  .fetch()
```

- Post 조회 메서드 12개 모두에 Company fetchJoin 적용
- 검색 쿼리에도 동일한 패턴 적용

### BatchSize를 통한 ElementCollection 최적화

```kotlin
@BatchSize(size = 100)
@ElementCollection(fetch = FetchType.LAZY, targetClass = Category::class)
@CollectionTable(name = "post_categories", joinColumns = [JoinColumn(name = "post_id")])
@Enumerated(EnumType.STRING)
@Column(name = "category", nullable = false)
var categories: MutableSet<Category> = mutableSetOf()
```

- categories 로딩 시 N+1을 `1 + ceil(N/100)` 쿼리로 완화
- 1000개 Post 조회 시 1000회 → 11회로 쿼리 감소 (99.5% 감소)

### DTO Projection 활용

집계 쿼리에서 불필요한 엔티티 로딩을 방지하기 위해 Querydsl Projection을 활용했습니다.

```kotlin
.select(
  Projections.constructor(
    CompanyPostSummaryDto::class.java,
    company.id, company.name, company.blogUrl,
    company.logoImageName, company.totalViewCount,
    post.id.count(), post.publishedAt.max()
  )
)
```

---

## 3. 벡터 검색 성능 최적화

### 아키텍처 설계

PostgreSQL의 pgvector 확장을 활용하여 3072차원 임베딩 벡터 기반의 유사도 검색을 구현했습니다.

```kotlin
@Entity
@Table(name = "post_embedding")
class PostEmbedding(
    @Id
    @Column(name = "post_id")
    val postId: Long,

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 3072)
    @Column(name = "embedding_vector")
    val embeddingVector: FloatArray
) : BaseEntity()
```

### 검색 로직

사용자의 최근 읽음 이력(10개)의 임베딩 벡터를 평균화하여 개인화된 추천 쿼리를 생성합니다.

```sql
SELECT * FROM post_embedding
WHERE post_id NOT IN :excludeIds
ORDER BY embedding_vector <-> CAST(:targetVector AS vector)
LIMIT :limit
```

### 핵심 포인트

- **L2 Distance (`<->`)**: 유클리드 거리 기반 유사도 측정
- **평균 벡터 기법**: 다수의 관심사를 단일 벡터로 응축하여 쿼리 10회 → 1회로 감소
- **제외 필터**: 이미 읽은 게시글을 결과에서 배제하여 추천 품질 향상

---

## 4. 조회수 집계 트랜잭션 최적화

### 문제 상황

기존 `recordView()` 메서드에서 중복 체크, PostView 저장, Post 조회수 증가, Company 총 조회수 증가가 단일 트랜잭션으로 묶여 있었습니다. 트랜잭션 범위가 과도하게 넓어지면서 DB Lock 경합이 발생하고, 조회수 업데이트 실패 시 전체 조회 기록이 롤백되는 문제가 있었습니다.

### 해결 방안

Spring Event와 `@TransactionalEventListener`를 활용하여 관심사를 분리했습니다.

```kotlin
@Service
class PostViewService(
  private val postViewRepository: PostViewRepository,
  private val eventPublisher: ApplicationEventPublisher
) {
  @Transactional
  fun recordView(postId: Long, companyId: Long, ipAddress: String) {
    // 1. 중복 체크 및 조회 기록 저장 (트랜잭션 내)
    if (!postViewRepository.existsByPostIdAndIpAddress(postId, ipAddress)) {
      postViewRepository.save(PostView(postId, ipAddress))

      // 2. 조회수 증가 이벤트 발행 (트랜잭션 커밋 후 실행)
      eventPublisher.publishEvent(
        ViewCountIncrementEvent(postId, companyId)
      )
    }
  }
}

@Component
class ViewCountEventHandler(
  private val viewCountUpdater: ViewCountUpdater,
  private val companyViewCountUpdater: CompanyViewCountUpdater
) {
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  fun handleViewCountIncrement(event: ViewCountIncrementEvent) {
    try {
      viewCountUpdater.incrementViewCount(event.postId)
      companyViewCountUpdater.incrementTotalViewCount(event.companyId)
    } catch (e: Exception) {
      logger.error(e) { "Failed to increment view count" }
    }
  }
}
```

### 핵심 포인트

- **트랜잭션 분리**: 조회 기록 저장과 카운트 증가를 별도 트랜잭션으로 분리
- **AFTER_COMMIT**: 조회 기록이 성공적으로 커밋된 후에만 카운트 증가 실행
- **@Async**: 비동기 처리로 사용자 응답 지연 방지
- **장애 격리**: 카운트 증가 실패가 조회 기록에 영향을 주지 않음
- **Eventual Consistency**: 조회수는 최종적으로 일관성을 보장

---

## 5. Gemini API Rate Limit 관리

### Resilience4j 기반 Rate Limiting

외부 API 호출의 안정성을 위해 Resilience4j RateLimiter와 Circuit Breaker를 적용했습니다.
설정은 `application.yml`에서 외부화하여 환경별로 유연하게 조정할 수 있습니다.

```kotlin
@Configuration
@EnableConfigurationProperties(RateLimiterProperties::class, CircuitBreakerProperties::class)
class ResilienceConfig(
  private val rateLimiterProperties: RateLimiterProperties,
  private val circuitBreakerProperties: CircuitBreakerProperties
) {
  @Bean
  fun rateLimiterRegistry(): RateLimiterRegistry {
    val geminiConfig = createRateLimiterConfig(rateLimiterProperties.gemini)
    val registry = RateLimiterRegistry.of(geminiConfig)

    // 용도별 Rate Limiter 등록
    registry.rateLimiter("geminiArticleSummarizer", geminiConfig)
    registry.rateLimiter("geminiBatchRpm", createRateLimiterConfig(rateLimiterProperties.geminiBatchRpm))
    registry.rateLimiter("geminiBatchRpd", createRateLimiterConfig(rateLimiterProperties.geminiBatchRpd))
    registry.rateLimiter("geminiEmbedding", createRateLimiterConfig(rateLimiterProperties.geminiEmbedding))

    // 도메인별 크롤링 Rate Limiter
    registry.rateLimiter("woowahan", createRateLimiterConfig(rateLimiterProperties.crawler.conservative))
    registry.rateLimiter("defaultCrawler", createRateLimiterConfig(rateLimiterProperties.crawler.default))

    return registry
  }

  @Bean
  fun circuitBreakerRegistry(): CircuitBreakerRegistry {
    val registry = CircuitBreakerRegistry.ofDefaults()
    val batchConfig = CircuitBreakerConfig.custom()
      .failureRateThreshold(circuitBreakerProperties.geminiBatch.failureRateThreshold.toFloat())
      .slowCallRateThreshold(circuitBreakerProperties.geminiBatch.slowCallRateThreshold.toFloat())
      .waitDurationInOpenState(Duration.ofSeconds(circuitBreakerProperties.geminiBatch.waitDurationInOpenStateSeconds))
      .slidingWindowSize(circuitBreakerProperties.geminiBatch.slidingWindowSize)
      .build()

    registry.circuitBreaker("geminiBatch", batchConfig)
    return registry
  }
}
```

### 핵심 포인트

- **설정 외부화**: `RateLimiterProperties`, `CircuitBreakerProperties`로 환경별 설정 분리
- **용도별 Rate Limiter**: 요약(RPM/RPD), 임베딩, 크롤링 각각 별도 제한
- **Circuit Breaker**: 연속 실패 시 빠른 실패 반환으로 시스템 보호
- **도메인별 차등 제한**: 크롤링 대상 도메인별로 보수적/기본 Rate Limit 구분 적용

---

## 6. 스트리밍 기반 타임아웃 방지

### 문제 상황

Gemini API의 배치 요약 응답은 대용량 JSON 배열로 반환됩니다. 전체 응답을 기다리면:
- 응답 완료까지 대기 시간 증가로 타임아웃 발생
- 부분 실패 시 전체 재시도 필요
- 사용자에게 결과 전달 지연

### 해결 방안

스트리밍 방식으로 JSON 객체를 실시간 파싱하여 처리합니다.

```kotlin
class StreamingJsonParser {
    companion object {
        private const val OBJECT_START = '{'
        private const val OBJECT_END = '}'
        private const val QUOTE = '"'
        private const val ESCAPE = '\\'
    }

    private var buffer = ""

    fun process(chunk: String): List<SummaryResultWithId> {
        buffer += chunk
        val summaries = mutableListOf<SummaryResultWithId>()

        while (true) {
            val startIndex = buffer.indexOf(OBJECT_START)
            if (startIndex == -1) break

            val endIndex = findEndOfObject(startIndex)
            if (endIndex != -1) {
                val jsonObject = buffer.substring(startIndex, endIndex + 1)
                val summary = parseSummary(jsonObject)

                if (summary != null) {
                    summaries.add(summary)
                    buffer = buffer.substring(endIndex + 1)
                } else {
                    buffer = buffer.substring(startIndex + 1)
                }
            } else {
                break  // 불완전한 객체 - 다음 청크 대기
            }
        }
        return summaries
    }

    private fun findEndOfObject(startIndex: Int): Int {
        var depth = 0
        var isInsideQuotes = false
        var isEscaped = false

        for (i in startIndex until buffer.length) {
            val char = buffer[i]

            if (char == QUOTE && !isEscaped) {
                isInsideQuotes = !isInsideQuotes
            }

            if (!isInsideQuotes) {
                when (char) {
                    OBJECT_START -> depth++
                    OBJECT_END -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }

            isEscaped = (char == ESCAPE && !isEscaped)
        }
        return -1  // 불완전한 객체
    }
}
```

### 핵심 포인트

- **실시간 파싱**: 청크 단위로 수신하며 완성된 JSON 객체 즉시 처리
- **타임아웃 방지**: 전체 응답 대기 없이 중간 결과부터 처리하여 연결 유지
- **부분 성공**: 일부 객체 파싱 실패해도 나머지 객체는 정상 처리
- **중첩 구조 처리**: depth 추적으로 중첩 객체/문자열 내 괄호 올바르게 처리

---

## 7. RSS/Atom 피드 파싱 및 중복 처리

### Strategy Pattern 기반 파서 설계

다양한 피드 형식(RSS 2.0, Atom 1.0)과 기업별 커스텀 구조를 유연하게 처리하기 위해 전략 패턴을 적용했습니다.

```
FeedTypeStrategyResolver
├── RssFeedStrategy      (RSS 2.0)
└── AtomFeedStrategy     (Atom 1.0)

BlogParserResolver
├── FeedParser           (일반 RSS/Atom)
├── OliveYoungBlogParser (특수 처리)
└── ElevenStBlogParser   (특수 처리)
```

### 중복 감지 전략

URL 기반의 Idempotent 처리로 크롤링 시 중복 게시글을 방지합니다.

```kotlin
val existUrls = postRepository.findAllByUrlIn(originalUrls).map { it.url }.toSet()
val filteredPosts = allPosts.filter { it.url !in existUrls }
```

### 도메인별 컨텐츠 추출

13개 기업 기술블로그의 HTML 구조를 분석하여 도메인별 CSS 선택자를 매핑했습니다.

```kotlin
private val contentSelectorMap = mapOf(
  "techblog.woowahan.com"   to ".post-content-inner > .post-content-body",
  "tech.kakao.com"          to ".inner_content > .daum-wm-content.preview",
  "toss.tech"               to "article.css-hvd0pt > div.css-1vn47db",
  "d2.naver.com"            to ".post-area, .section-content, .post-body",
  "techblog.lycorp.co.jp"   to "article.bui_component > div.post_content_wrap > div.content_inner > div.content",
  "blog.banksalad.com"      to "div[class^=postDetailsstyle__PostDescription]",
  "aws.amazon.com"          to "article.blog-post section.blog-post-content[property=articleBody]",
  "hyperconnect.github.io"  to "article.post .post-content.e-content",
  "helloworld.kurly.com"    to ".post-content, .article-body, .post",
  "tech.socarcorp.kr"       to ".post-content, .article-body, .post",
  "dev.gmarket.com"         to ".post-content, .article-body, .post",
  "medium.com"              to "article, .meteredContent, .pw-post-body, .postArticle-content",
  "oliveyoung.tech"         to "div.blog-post-content"
)
```

### 컨텐츠 추출 아키텍처

```kotlin
class WebContentExtractor(
  private val selectorRegistry: ContentSelectorRegistry,
  private val textExtractor: HtmlTextExtractor
) : ContentExtractor {
  override fun extract(url: String, fallbackContent: String): String {
    return runCatching {
      val domain = extractDomain(url)
      val document = fetchDocument(url)
      val selectors = selectorRegistry.getSelectors(domain)

      selectors.firstNotNullOfOrNull { selector ->
        document.selectFirst(selector)?.let { element ->
          textExtractor.extract(element).takeIf { it.isNotBlank() }
        }
      } ?: fallbackContent
    }.getOrElse { fallbackContent }
  }
}
```

---

## 8. 배치 요약 검증

### 문제 상황

AI 모델의 응답은 예측 불가능하며, 다음과 같은 문제가 발생할 수 있습니다:
- ID 불일치 (요청 ID와 응답 ID가 다름)
- 요약 품질 미달 (너무 짧거나 너무 긴 요약)
- 유효하지 않은 카테고리 반환
- 필수 필드 누락

### 해결 방안

`BatchSummaryValidator`를 통해 AI 응답의 품질을 검증합니다.

```kotlin
@Component
class BatchSummaryValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    fun validate(
        input: ArticleInput,
        result: SummaryResultWithId,
        validCategories: Set<String>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // 1. ID 매칭 검증
        if (result.id != input.id) {
            errors.add("ID mismatch: expected ${input.id}, got ${result.id}")
        }

        // 2. 성공 플래그 확인
        if (!result.success) {
            errors.add("Result marked as failed: ${result.error}")
            return ValidationResult(false, errors)
        }

        // 3. 요약 품질 검증
        when {
            result.summary.isNullOrBlank() -> errors.add("Summary is blank")
            result.summary.length < 50 -> errors.add("Summary too short: ${result.summary.length} chars")
            result.summary.length > 5000 -> errors.add("Summary too long: ${result.summary.length} chars")
        }

        // 4. 카테고리 유효성 검증
        if (result.categories.isNullOrEmpty()) {
            errors.add("No categories provided")
        } else {
            val invalidCategories = result.categories.filterNot { it in validCategories }
            if (invalidCategories.isNotEmpty()) {
                errors.add("Invalid categories: $invalidCategories")
            }
        }

        // 5. Preview 검증
        if (result.preview.isNullOrBlank()) {
            errors.add("Preview is blank")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}
```

### 핵심 포인트

- **ID 매칭**: 요청과 응답의 ID 일치 검증으로 데이터 무결성 보장
- **길이 검증**: 너무 짧거나 긴 요약 필터링
- **카테고리 화이트리스트**: 사전 정의된 카테고리만 허용
- **에러 누적**: 단일 실패가 아닌 모든 검증 오류 수집

---

## 9. 요약 실패 관리 및 재시도

### 문제 상황

AI 요약 과정에서 다양한 실패가 발생할 수 있습니다:
- API Rate Limit 초과
- 네트워크 타임아웃
- 응답 파싱 실패
- 검증 실패

단순 재시도는 동일한 실패를 반복하므로 지능적인 재시도 전략이 필요합니다.

### 해결 방안

#### 1. 실패 이력 추적 (PostSummaryFailure)

```kotlin
@Entity
@Table(name = "post_summary_failures")
class PostSummaryFailure(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "error_type", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    val errorType: SummaryErrorType,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(name = "failed_at", nullable = false)
    val failedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "batch_size")
    val batchSize: Int? = null,

    @Column(name = "is_batch_failure", nullable = false)
    val isBatchFailure: Boolean = false
)
```

#### 2. 지수 백오프 재시도 큐 (SummaryRetryQueue)

```kotlin
@Entity
@Table(name = "summary_retry_queue")
class SummaryRetryQueue(
    @Id
    val postId: Long,

    @Column(length = 1000)
    val reason: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    val errorType: ErrorType,

    val retryCount: Int = 0,
    val nextRetryAt: Instant,
    val createdAt: Instant = Instant.now(),
    val lastRetryAt: Instant? = null,
    val maxRetries: Int = 5
) {
    fun shouldRetry(): Boolean {
        return retryCount < maxRetries && Instant.now().isAfter(nextRetryAt)
    }

    fun incrementRetry(): SummaryRetryQueue {
        return SummaryRetryQueue(
            postId = postId,
            reason = reason,
            errorType = errorType,
            retryCount = retryCount + 1,
            // 지수 백오프: 5분 * 2^retryCount
            nextRetryAt = Instant.now().plusSeconds(300L * (1 shl retryCount)),
            createdAt = createdAt,
            lastRetryAt = Instant.now(),
            maxRetries = maxRetries
        )
    }
}
```

#### 3. Post 엔티티 실패 카운트

```kotlin
@Entity
@Table(name = "posts")
class Post(
    // ... 기존 필드 ...

    @Column(name = "summary_failure_count", nullable = false)
    var summaryFailureCount: Int = 0
) : BaseEntity()
```

### 재시도 전략

| 재시도 횟수 | 대기 시간 | 누적 시간 |
|------------|----------|----------|
| 1회차 | 5분 | 5분 |
| 2회차 | 10분 | 15분 |
| 3회차 | 20분 | 35분 |
| 4회차 | 40분 | 75분 |
| 5회차 | 80분 | 155분 |

### 핵심 포인트

- **지수 백오프**: 실패 횟수에 따라 재시도 간격 증가로 API 부하 방지
- **실패 이력 보존**: 분석 및 디버깅을 위한 모든 실패 기록
- **최대 재시도 제한**: 무한 재시도 방지 (기본 5회)
- **에러 유형 분류**: 에러 타입별 다른 처리 전략 적용 가능
