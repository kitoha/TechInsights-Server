# Tech Insights - Backend Service

## 프로젝트 개요

Tech Insights는 최신 IT 기술 관련 회사들의 기술 블로그 게시글을 모아 보여주는 플랫폼입니다. 사용자는 다양한 카테고리별 최신 글과 인기 글을 탐색할 수 있으며, 회사별 블로그 요약, 게시글 상세 보기, 댓글 작성, 좋아요 등의 기능을 제공합니다.

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
  - 사용자의 IP를 기반으로 조회수를 집계하여 게시글의 인기도를 측정합니다.

---

## 🚀 Quick Start

### Prerequisites

- **JDK 21**
- **Docker & Docker Compose**
- **Gradle 8.5+** (Wrapper 포함)

### 환경 설정

프로젝트 루트에 `.env` 파일을 생성합니다:

```env
DB_PASSWORD=your_password
GEMINI_API_KEY=your_gemini_api_key
```

### 로컬 실행

```bash
# 1. 저장소 클론
git clone https://github.com/kitoha/TechInsights-Server.git
cd TechInsights-Server

# 2. DB 실행 (PostgreSQL + pgvector)
docker-compose -f docker-compose.db.yml up -d

# 3. 빌드
./gradlew clean build

# 4. API 서버 실행
./gradlew :api:bootRun
```

API 서버: http://localhost:8080

Health Check: http://localhost:8080/actuator/health

---

## 🏗 프로젝트 구조

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
├── docker-compose.yml       # 로컬 전체 환경 (API + DB + Nginx)
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

## 🐳 Docker

| 파일 | 용도 | 명령어 |
|------|------|--------|
| `docker-compose.db.yml` | PostgreSQL + pgvector | `docker-compose -f docker-compose.db.yml up -d` |
| `docker-compose.yml` | 전체 로컬 환경 | `docker-compose up -d` |
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

## 🧪 테스트

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

커버리지 리포트: `build/reports/jacoco/test/html/index.html`

### 테스트 스택
- **JUnit 5** - 테스트 프레임워크
- **Kotest** - Kotlin 테스트 라이브러리
- **MockK** - Kotlin 모킹 라이브러리
- **JaCoCo** - 코드 커버리지

---

## 📡 API Endpoints

### Post API

| Method | Endpoint                      | Description                  |
|--------|-------------------------------|------------------------------|
| `GET`  | `/api/v1/posts`               | 게시글 목록 조회 (페이징, 정렬, 카테고리 필터) |
| `GET`  | `/api/v1/posts/{postId}`      | 게시글 상세 조회                    |
| `POST` | `/api/v1/posts/{postId}/view` | 조회수 기록                       |

### Search API

| Method | Endpoint                 | Description            |
|--------|--------------------------|------------------------|
| `GET`  | `/api/v1/search/instant` | 실시간 검색 (자동완성)          |
| `GET`  | `/api/v1/search`         | 상세 검색 (페이징, 정렬, 회사 필터) |

### Company API

| Method | Endpoint                         | Description    |
|--------|----------------------------------|----------------|
| `GET`  | `/api/v1/companies`              | 회사 목록 조회       |
| `GET`  | `/api/v1/companies/{companyId}`  | 회사 상세 조회       |
| `GET`  | `/api/v1/companies/top-by-views` | 조회수 기준 상위 회사   |
| `GET`  | `/api/v1/companies/top-by-posts` | 게시글 수 기준 상위 회사 |
| `GET`  | `/api/v1/companiesSummaries`     | 회사별 게시글 통계     |

### Category API

| Method | Endpoint                     | Description  |
|--------|------------------------------|--------------|
| `GET`  | `/api/v1/categories/summary` | 카테고리별 게시글 통계 |

### Recommendation API

| Method | Endpoint                  | Description   |
|--------|---------------------------|---------------|
| `GET`  | `/api/v1/recommendations` | AI 기반 개인화 추천  |

---

## 시스템 아키텍처

API Sever

<img width="4920" height="1272" alt="Image" src="https://github.com/user-attachments/assets/39001247-3359-46c5-a12d-33dda77f3456" />

Batch Server

<img width="3930" height="2197" alt="Image" src="https://github.com/user-attachments/assets/664b2b9e-2c79-4fd1-9bbe-acd050342ca5" />


---

## ERD

```mermaid
erDiagram
    Company {
        Long id PK
        String name "회사명"
        String blogUrl "블로그 주소"
        String logoImageName "로고 이미지"
        Boolean rssSupported "RSS 지원 여부"
        Long totalViewCount "총 조회수"
        Long postCount "게시물 수"
        LocalDateTime createdAt "생성일"
        LocalDateTime updatedAt "수정일"
        LocalDateTime deletedAt "삭제일 (soft delete)"
    }

    Post {
        Long id PK
        String title "제목"
        String preview "미리보기"
        String url "원본 링크"
        String content "내용"
        LocalDateTime publishedAt "게시일"
        String thumbnail "썸네일"
        Long company_id FK "회사 ID"
        Long viewCount "조회수"
        Boolean isSummary "요약 여부"
        Boolean isEmbedding "임베딩 여부"
        Int summaryFailureCount "요약 실패 횟수"
        LocalDateTime createdAt "생성일"
        LocalDateTime updatedAt "수정일"
        LocalDateTime deletedAt "삭제일 (soft delete)"
    }

    post_categories {
        Long post_id FK "게시물 ID"
        String category "카테고리 (Enum)"
    }

    PostEmbedding {
        Long postId PK "게시물 ID (FK)"
        String companyName "회사명"
        String categories "카테고리"
        String content "내용"
        FloatArray embeddingVector "임베딩 벡터 (3072차원)"
        LocalDateTime createdAt "생성일"
        LocalDateTime updatedAt "수정일"
    }

    PostView {
        Long id PK
        Long postId FK "게시물 ID"
        String userOrIp "사용자 또는 IP"
        LocalDate viewedDate "조회일"
        String userAgent "User Agent"
        LocalDateTime createdAt "생성일"
        LocalDateTime updatedAt "수정일"
    }

    AnonymousUserReadHistory {
        Long id PK
        String anonymousId "익명 사용자 ID"
        Long postId FK "게시물 ID"
        LocalDateTime readAt "읽은 시간"
        LocalDateTime createdAt "생성일"
        LocalDateTime updatedAt "수정일"
    }

    PostSummaryFailure {
        Long id PK
        Long postId FK "게시물 ID"
        String errorType "에러 유형 (Enum)"
        String errorMessage "에러 메시지"
        LocalDateTime failedAt "실패 시간"
        Int batchSize "배치 크기"
        Boolean isBatchFailure "배치 실패 여부"
    }

    SummaryRetryQueue {
        Long postId PK "게시물 ID"
        String reason "재시도 사유"
        String errorType "에러 유형 (Enum)"
        Int retryCount "재시도 횟수"
        Instant nextRetryAt "다음 재시도 시간"
        Instant createdAt "생성일"
        Instant lastRetryAt "마지막 재시도 시간"
        Int maxRetries "최대 재시도 횟수"
    }

    Company ||--o{ Post : "owns"
    Post }|..|| PostEmbedding : "has one"
    Post ||--o{ PostView : "has many"
    Post ||--o{ AnonymousUserReadHistory : "has many"
    Post ||--o{ post_categories : "has many"
    Post ||--o{ PostSummaryFailure : "has many"
    Post ||--o| SummaryRetryQueue : "has one"
```

---

## 🛠 Tech Stack

### Language

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)

### Framework & Runtime

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring%20Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![JPA](https://img.shields.io/badge/JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![Querydsl](https://img.shields.io/badge/Querydsl-4695EB?style=for-the-badge&logo=java&logoColor=white)

### Database

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![pgvector](https://img.shields.io/badge/pgvector-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)

### AI / ML

![Google Gemini](https://img.shields.io/badge/Google%20Gemini-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)

### Infra & DevOps

![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white)

### Runtime

![JDK 21](https://img.shields.io/badge/JDK-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

---

## Technical Challenges & Solutions

개발 과정에서 마주한 기술적 도전과 해결 방안을 정리했습니다.

| 주제 | 핵심 기술 |
|------|----------|
| [조회수 집계 트랜잭션 최적화](./TECHNICAL_CHALLENGES.md#1-조회수-집계-트랜잭션-최적화) | Spring Event, @TransactionalEventListener, Eventual Consistency |
| [벡터 검색 성능 최적화](./TECHNICAL_CHALLENGES.md#2-벡터-검색-성능-최적화) | PostgreSQL pgvector, L2 Distance, 평균 벡터 기법 |
| [RSS/Atom 피드 파싱](./TECHNICAL_CHALLENGES.md#3-rssatom-피드-파싱-및-중복-처리) | Strategy Pattern, 도메인별 CSS 셀렉터, URL 기반 중복 감지 |
| [Gemini API Rate Limit 관리](./TECHNICAL_CHALLENGES.md#4-gemini-api-rate-limit-관리) | Resilience4j RateLimiter, Circuit Breaker, 설정 외부화 |
| [N+1 쿼리 최적화](./TECHNICAL_CHALLENGES.md#5-n1-쿼리-최적화) | Querydsl fetchJoin, BatchSize, DTO Projection |
| [스트리밍 JSON 파싱](./TECHNICAL_CHALLENGES.md#6-스트리밍-json-파싱) | 실시간 청크 파싱, 메모리 효율화, 부분 응답 처리 |
| [배치 요약 검증](./TECHNICAL_CHALLENGES.md#7-배치-요약-검증) | AI 응답 품질 검증, ID 매칭, 카테고리 유효성 |
| [요약 실패 관리 및 재시도](./TECHNICAL_CHALLENGES.md#8-요약-실패-관리-및-재시도) | 지수 백오프, 실패 이력 추적, 재시도 큐 |

상세 내용은 [TECHNICAL_CHALLENGES.md](./TECHNICAL_CHALLENGES.md)를 참고하세요.

---

## Preview

| 검색 기능                                                            | 세부 페이지                                                                | 다크모드                                                              |
|------------------------------------------------------------------|-----------------------------------------------------------------------|-------------------------------------------------------------------|
| <img src="./img/gif/Search_Test.gif" alt="검색 기능 데모" width="300"> | <img src="./img/gif/DetailView_Test.gif" alt="세부 페이지 데모" width="300"> | <img src="./img/gif/DarkMode_Test.gif" alt="다크모드 데모" width="300"> |

---

## Initial Design

![Image](https://github.com/user-attachments/assets/d5533bfa-e6cb-46af-9c32-16a3d9b98aa0)

---

## 접속 링크

https://www.techinsights.shop/
