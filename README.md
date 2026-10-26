# Tech Insights - Backend Service

## 프로젝트 개요

Tech Insights는 최신 IT 기술 관련 회사들의 기술 블로그 게시글을 모아 보여주는 플랫폼입니다. 사용자는 다양한 카테고리별 최신 글과 인기 글을 탐색할 수 있으며, 회사별 블로그 요약, 게시글 상세 보기, 좋아요 등의 기능을 제공합니다.

---

### Tech Stack

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.2-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-batch)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![pgvector](https://img.shields.io/badge/pgvector-0.8+-4169E1?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![JDK](https://img.shields.io/badge/JDK-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)

### AI & Libraries

[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-API-8E75B2?logo=googlegemini&logoColor=white)](https://ai.google.dev/)
[![JPA](https://img.shields.io/badge/JPA-Hibernate-59666C?logo=hibernate&logoColor=white)](https://hibernate.org/)
[![Querydsl](https://img.shields.io/badge/Querydsl-5.1-4695EB)](https://querydsl.com/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-2.3.0-FF6B6B)](https://resilience4j.readme.io/)

### Testing & Quality

[![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![Kotest](https://img.shields.io/badge/Kotest-5.8-6FB536)](https://kotest.io/)
[![Mockk](https://img.shields.io/badge/Mockk-1.13-4ECDC4)](https://mockk.io/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-1.21-2496ED?logo=testcontainers&logoColor=white)](https://testcontainers.com/)
[![Jacoco](https://img.shields.io/badge/Coverage-Jacoco-green)](https://www.jacoco.org/)
[![SonarCloud](https://img.shields.io/badge/Quality-SonarCloud-4E9BCD?logo=sonarcloud&logoColor=white)](https://sonarcloud.io/)

### DevOps & Infrastructure

[![Nginx](https://img.shields.io/badge/Nginx-latest-009639?logo=nginx&logoColor=white)](https://nginx.org/)
[![GitHub Actions](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=github-actions&logoColor=white)](https://github.com/features/actions)
[![AWS](https://img.shields.io/badge/AWS-Deploy-232F3E?logo=amazonwebservices&logoColor=white)](https://aws.amazon.com/)
[![Gradle](https://img.shields.io/badge/Gradle-8.5-02303A?logo=gradle&logoColor=white)](https://gradle.org/)

### Architecture & Design Patterns

[![Multi Module](https://img.shields.io/badge/Architecture-Multi%20Module-FF6B6B)]()
[![Event Driven](https://img.shields.io/badge/Pattern-Event%20Driven-4ECDC4)]()
[![Batch Processing](https://img.shields.io/badge/Pattern-Batch%20Processing-F38181)]()
[![Vector Search](https://img.shields.io/badge/Feature-Vector%20Search-9B59B6)]()

---
## Preview

| 검색 기능                                                            | 세부 페이지                                                                | 다크모드                                                              |
|------------------------------------------------------------------|-----------------------------------------------------------------------|-------------------------------------------------------------------|
| <img src="./img/gif/Search_Test.gif" alt="검색 기능 데모" width="300"> | <img src="./img/gif/DetailView_Test.gif" alt="세부 페이지 데모" width="300"> | <img src="./img/gif/DarkMode_Test.gif" alt="다크모드 데모" width="300"> |

---

## 주요 기능

- 기업별 기술블로그 피드
  - 각 기업의 최신 기술 아티클을 수집하여 요약 및 원문 링크를 제공합니다.
  - 조회수, 게시물 수 기반으로 주목받는 기업 랭킹을 확인할 수 있습니다.

- 콘텐츠 탐색 및 검색
  - AI, Backend, Frontend 등 기술 카테고리별로 게시글을 필터링하여 볼 수 있습니다.
  - 실시간 검색으로 빠르게 원하는 정보를 찾고, 관련도순/최신순으로 정렬하는 상세 검색을 지원합니다.

- AI 기반 개인화 추천
  - 사용자의 콘텐츠 조회 이력을 바탕으로 AI가 흥미로워할 만한 아티클을 개인화하여 추천합니다.
  - 게시글의 의미를 분석한 벡터 검색을 통해 관련성 높은 콘텐츠를 제공합니다.

- 인사이트 및 통계
  - 인기글, AI 추천글 등 다양한 기준으로 집계된 통계 정보를 제공합니다.
  - 카테고리별 게시글 수, 기업별 포스팅 현황 등을 차트로 시각화하여 보여줍니다.

- 상세 보기 및 조회수
  - 각 아티클의 AI 요약, 태그, 원문 링크 등 상세 정보를 확인할 수 있습니다.
  - 로그인 사용자는 사용자 ID, 비로그인 사용자는 `aid` 쿠키를 기준으로 게시글당 하루 한 번만 조회수를 집계해 인기도를 측정합니다.

---

## 시스템 아키텍처

### 전체 인프라 (AWS)

<img src="./img/architecture/techinsights-aws.drawio.png" alt="TechInsights AWS 인프라 구성도" width="100%">

배포 환경의 전체 구성입니다. 브라우저 트래픽은 Vercel BFF와 Cloudflare를 거쳐 nginx로 들어옵니다. nginx에서 API 서버에 도달하는 경로와 배치 서버·DB의 위치를 확인할 수 있습니다.

### API 서버 요청 흐름

<img src="./img/diagrams/api-server.png" alt="API 서버 요청 처리 흐름도" width="100%">

요청이 nginx, 보안 필터 체인, 컨트롤러, 도메인 서비스를 거쳐 DB와 Gemini API에 이르는 흐름입니다. 에러가 어느 계층에서 어떤 응답으로 바뀌는지는 아래 표에 정리했습니다.

**에러 응답 매핑**

| 상황 | 발생 위치 | 응답 |
|------|----------|------|
| Gemini 호출 속도 제한 초과 (Resilience4j `RequestNotPermitted`) | `GlobalExceptionHandler` | 429 `TOO_MANY_REQUESTS` (problem+json) |
| 서킷 브레이커 open (`CallNotPermittedException`) | `GlobalExceptionHandler` | 503 `SERVICE_UNAVAILABLE` (problem+json) |
| `/api/v1/auth/refresh`·`logout` 요청 제한 초과 (IP당 분당 10회, burst 5) | `nginx.prod.conf` | 429 `TOO_MANY_REQUESTS` (problem+json) |
| `/api/v1/search` 요청 제한 초과 (IP당 분당 30회, burst 10) | `nginx.prod.conf` | 429 (nginx 기본 응답 본문, problem+json 아님) |
| 업스트림 502/504 | `nginx.prod.conf` | 503 `SERVICE_UNAVAILABLE` 점검 안내 (problem+json) |
| 허용되지 않은 `Origin`의 상태 변경 요청 | Spring Security `CorsFilter` | 403 (일반 텍스트) |
| `Origin` 없이 허용되지 않은 `Referer`로 온 상태 변경 요청 | `CsrfOriginFilter` | 403 `CSRF_ORIGIN_NOT_ALLOWED` (problem+json) |
| 액세스 토큰 없음·만료·위조 | `ProblemDetailAuthenticationEntryPoint` | 401 `UNAUTHORIZED` / `EXPIRED_TOKEN` / `INVALID_TOKEN` / `TOKEN_TAMPERED` |
| 권한 부족 | `ProblemDetailAccessDeniedHandler` | 403 `FORBIDDEN` |
| 도메인 예외 (예: 게시글·사용자 없음, 닉네임 중복) | `GlobalExceptionHandler` | 404 / 409 / 400 등 4xx (problem+json) |

### 배치 데이터 파이프라인

<img src="./img/diagrams/batch-pipeline.png" alt="배치 데이터 파이프라인 흐름도" width="100%">

RSS 크롤링, Gemini 요약, 임베딩 생성, GitHub 트렌딩 수집이 이어지는 파이프라인입니다. 각 단계의 결과가 다음 단계(요약 → 임베딩)의 입력이 되는 순서를 확인할 수 있습니다.

<details>
<summary>배치 Job 일정</summary>

실행 시각은 `.github/workflows/batch-*.yml`의 cron(UTC)이며, 앱 안에서 스케줄링하지 않습니다.

| Job | 일정 (UTC) | reader → processor → writer | 비고 |
|-----|-----------|----------------------------|------|
| `githubTrendingJob` | 매일 00:00 | `GithubRepoReader` → `GithubRepoProcessor` → `GithubRepoWriter` | `DataIntegrityViolationException` 건너뛰기(최대 10) |
| `githubReadmeSummaryJob` | 매일 01:00 | `UnsummarizedRepoReader` → `GithubReadmeFetchProcessor` → `GithubReadmeBatchSummaryWriter` | 쿼터 키 `geminiReadmeRpd`. fault-tolerant 설정 없음 |
| `githubReadmeEmbeddingJob` | 매일 05:00 | `UnembeddedRepoReader` → `GithubReadmeEmbeddingProcessor` → `GithubReadmeEmbeddingWriter` | 쿼터 키 `geminiEmbeddingRpd`. 재시도 3회, 건너뛰기 최대 10 |
| `crawlPostJob` | 매일 06:00 | `CompanyReader` → `RawPostProcessor` → `RawPostWriter` | 재시도 3회(`batch.post-crawling.retry-limit`), 건너뛰기 최대 10 |
| `optimizedSummaryJob` | 매일 07:00 | `BatchAwarePostReader` → `AsyncBatchPostSummaryProcessor` → `FlatteningPostWriter`, 이어서 `CompanyPostCountReconcileTasklet` | 쿼터 키 `geminiBatchRpd`. 건너뛰기 최대 20 |
| `postVectorEmbeddingJob` | 매일 08:00 | `BatchSummarizedPostReader` → `BatchPostEmbeddingProcessor` → `BatchPostEmbeddingWriter` | 쿼터 키 `geminiEmbeddingRpd`. 재시도 3회, 건너뛰기 최대 10 |
| `communityCollectJob` | 워크플로 없음 | `CommunityCollectReader` → `CommunityCollectProcessor` → `CommunityCollectWriter` | fault-tolerant 설정 없음 |
| `communityAnalyzeJob` | 워크플로 없음 | `CommunityAnalyzeReader` → `CommunityAnalyzeProcessor` → `CommunityAnalyzeWriter` | 쿼터 키 `geminiCommunityRpd` |
| `refreshTokenCleanupJob` | 워크플로 없음 | `RefreshTokenCleanupTasklet` (tasklet) | 만료된 리프레시 토큰의 폐기 해시 정리 |

</details>

### 크롤링 시퀀스

<img src="./img/diagrams/crawling-sequence.png" alt="게시글 크롤링 시퀀스 다이어그램" width="100%">

`crawlPostJob`이 회사 목록을 읽어 게시글을 수집하고 저장하는 순서입니다. 호출 주체와 저장소 사이의 메시지 순서를 확인할 수 있습니다.

---

## ERD

스키마를 콘텐츠, 사용자·인증, GitHub 트렌딩의 세 영역으로 나누어 그렸습니다(`B001`~`V032` 마이그레이션 기준).

### 콘텐츠

<img src="./img/diagrams/erd-content.png" alt="콘텐츠 ERD: 회사, 게시글, 임베딩, 좋아요, 조회" width="100%">

회사, 게시글, 임베딩, 조회·좋아요 등 기술 블로그 콘텐츠 관련 테이블입니다.

### 사용자·인증

<img src="./img/diagrams/erd-users-auth.png" alt="사용자·인증 ERD: 사용자, 리프레시 토큰, 북마크" width="100%">

사용자, 리프레시 토큰 패밀리, 북마크 관련 테이블입니다.

### GitHub 트렌딩

<img src="./img/diagrams/erd-github.png" alt="GitHub 트렌딩 ERD: 저장소, README, 커뮤니티" width="100%">

GitHub 저장소, README 요약·임베딩, 커뮤니티 언급 관련 테이블입니다.

다이어그램에 그리지 않은 보조 테이블: `post_summary_failures`, `summary_retry_queue`, `anonymous_user_read_history`, `gemini_daily_usage`, Spring Batch 메타 테이블.

---

## Technical Challenges & Solutions

개발 과정에서 마주한 기술적 도전과 해결 방안을 정리했습니다.

| 주제 | 핵심 기술 | 성능 개선 |
|------|----------|----------|
| [배치 처리 아키텍처 고도화](./TECHNICAL_CHALLENGES.md#1-배치-처리-아키텍처-고도화) | 스트리밍 + DynamicBatchBuilder + 부분 성공 처리 | 일일 처리량 7배 향상 (20건 → 140건), 배치 처리 시간 84.6% 단축 |
| [N+1 쿼리 최적화](./TECHNICAL_CHALLENGES.md#2-n1-쿼리-최적화) | Querydsl fetchJoin, @BatchSize | 쿼리 99.5% 감소 (2001회 → 11회) |
| [벡터 검색 성능 최적화](./TECHNICAL_CHALLENGES.md#3-벡터-검색-성능-최적화) | pgvector, 코사인 거리(HNSW 인덱스), 평균 벡터 기법 | 쿼리 10회 → 1회 |
| [조회수 집계 트랜잭션 최적화](./TECHNICAL_CHALLENGES.md#4-조회수-집계-트랜잭션-최적화) | Spring Event, @TransactionalEventListener | Lock 경합 해소 |
| [Gemini API Rate Limit 관리](./TECHNICAL_CHALLENGES.md#5-gemini-api-rate-limit-관리) | Resilience4j (RateLimiter, Circuit Breaker) | API 호출 효율 5배 향상 (20회 → 4회/20건) |
| [스트리밍 기반 타임아웃 방지](./TECHNICAL_CHALLENGES.md#6-스트리밍-기반-타임아웃-방지) | generateContentStream, StreamingJsonParser | 타임아웃 발생률 83% 감소 (12% → 2%) |
| [RSS/Atom 피드 파싱](./TECHNICAL_CHALLENGES.md#7-rssatom-피드-파싱-및-중복-처리) | Strategy Pattern, URL 기반 중복 감지 | - |
| [배치 요약 검증](./TECHNICAL_CHALLENGES.md#8-배치-요약-검증) | AI 응답 품질 검증, ID 매칭 | - |
| [요약 실패 관리 및 재시도](./TECHNICAL_CHALLENGES.md#9-요약-실패-관리-및-재시도) | 지수 백오프, 실패 이력 추적 | 최대 재시도 7회 (즉시 2 + 배치 5) |

상세 내용은 [TECHNICAL_CHALLENGES.md](./TECHNICAL_CHALLENGES.md)를 참고하세요.

---

## Quick Start

### Prerequisites

- **JDK 21**
- **Docker & Docker Compose**
- Gradle은 Wrapper(8.5)를 사용합니다.

### 환경 설정

API 서버를 로컬에서 띄우려면 아래 환경 변수가 필요합니다. 기본값이 없어(`application.yml`, `application-local.yml`), 정의되어 있지 않으면 서버가 시작되지 않습니다.

| 변수 | 설명 |
|------|------|
| `JWT_SECRET_KEY` | Base64 문자열. 디코딩한 길이가 32바이트(256비트) 이상이어야 합니다. 예: `openssl rand -base64 32` |
| `GEMINI_API_KEY` | Google Gemini API 키 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth2 로그인용 클라이언트 정보 |

`DB_PASSWORD`는 서버 쪽에는 기본값(`local_password`)이 있지만 `docker-compose.db.yml`에서는 필수입니다. 컨테이너와 서버가 같은 값을 써야 하므로 둘 다 같은 값으로 맞추세요.

Spring Boot는 `.env`를 읽지 않습니다. Docker Compose는 프로젝트 루트의 `.env`에서 `DB_PASSWORD`를 읽으므로 `.env`에 적어 두고, `bootRun`을 실행하는 셸에는 `export`로 같은 값을 지정하세요.

```env
# .env (docker-compose가 읽음, git에는 포함되지 않음)
DB_PASSWORD=local_password
```

```bash
export DB_PASSWORD=local_password
export JWT_SECRET_KEY=$(openssl rand -base64 32)
export GEMINI_API_KEY=your_gemini_api_key
export GOOGLE_CLIENT_ID=your_google_client_id
export GOOGLE_CLIENT_SECRET=your_google_client_secret
```

기본 활성 프로파일은 `local`이며, DB 접속 정보 기본값은 `localhost:5432/techinsights_db`, 사용자 `local-user`입니다.

### 로컬 실행

```bash
# 1. 저장소 클론
git clone https://github.com/kitoha/TechInsights-Server.git
cd TechInsights-Server

# 2. DB 실행 (PostgreSQL 15 + pgvector)
docker-compose -f docker-compose.db.yml up -d

# 3. API 서버 실행 (Flyway 마이그레이션이 시작 시 자동 적용됩니다)
./gradlew :api:bootRun
```

API 서버: http://localhost:8080

Health Check: http://localhost:8080/actuator/health

배치 모듈(`batch`)은 앱 안에서 스케줄링되지 않고 `.github/workflows/batch-*.yml`(GitHub Actions)에서 실행됩니다.

---

## 프로젝트 구조

```
TechInsights-Server/
├── api/                     # REST API 모듈 (Spring Boot Web)
│   ├── src/main/kotlin/
│   └── Dockerfile
├── batch/                   # 배치 처리 모듈 (Spring Batch)
│   ├── src/main/kotlin/
│   └── Dockerfile
├── domain/                  # 공통 도메인 모듈 (JPA, Querydsl)
│   └── src/main/kotlin/
├── gradle/                  # Gradle 버전 카탈로그
│   └── libs.versions.toml
├── docker-compose.yml       # 로컬 전체 환경 (DB + API + Batch + Nginx)
├── docker-compose.db.yml    # DB만 실행
├── docker-compose.app.yml   # API + Nginx
└── docker-compose.prod.yml  # 프로덕션 환경
```

### 모듈 의존성

```
api ──┬──→ domain
batch ─┘
```

| 모듈 | 역할 |
|------|------|
| `api` | REST API 엔드포인트, 컨트롤러, 인증/인가 |
| `batch` | RSS 피드 크롤링, AI 요약, 임베딩 생성 |
| `domain` | 엔티티, 리포지토리, 도메인 서비스 |

---

## Docker

| 파일 | 용도 | 명령어 |
|------|------|--------|
| `docker-compose.db.yml` | PostgreSQL + pgvector | `docker-compose -f docker-compose.db.yml up -d` |
| `docker-compose.yml` | 전체 로컬 환경 (DB + API + Batch + Nginx). 외부 네트워크 `monitoring-network`가 필요합니다 | `docker network create monitoring-network` 후 `docker-compose up -d` |
| `docker-compose.app.yml` | API + Nginx | `docker-compose -f docker-compose.app.yml up -d` |
| `docker-compose.prod.yml` | 프로덕션 (AWS) | CodeDeploy로 실행 |

### Docker 이미지 빌드

```bash
# API 이미지 빌드
docker build -t techinsights-api ./api

# Batch 이미지 빌드
docker build -t techinsights-batch ./batch
```

---

## 테스트

리포지토리·마이그레이션 테스트가 Testcontainers로 PostgreSQL + pgvector 컨테이너를 띄우므로, **`./gradlew test`를 실행하려면 Docker가 실행 중이어야 합니다.**

```bash
# 전체 테스트 실행
./gradlew test

# 모듈별 테스트
./gradlew :api:test
./gradlew :batch:test
./gradlew :domain:test

# 커버리지 리포트 생성
./gradlew jacocoTestReport
```

커버리지 리포트: `<모듈>/build/reports/jacoco/test/html/index.html` (예: `api/build/reports/jacoco/test/html/index.html`)

### 테스트 스택
- **JUnit 5** - 테스트 프레임워크
- **Kotest** - Kotlin 테스트 라이브러리
- **MockK** - Kotlin 모킹 라이브러리
- **Testcontainers** - 실제 PostgreSQL + pgvector 컨테이너에서 리포지토리·마이그레이션 테스트
- **JaCoCo** - 코드 커버리지

---

## Initial Design

![Image](https://github.com/user-attachments/assets/d5533bfa-e6cb-46af-9c32-16a3d9b98aa0)

---

## 접속 링크

https://www.techinsights.shop/
