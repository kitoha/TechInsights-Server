# Tech Insights - Backend Service

## 프로젝트 개요

Tech Insights는 최신 IT 기술 관련 회사들의 기술 블로그 게시글을 모아 보여주는 플랫폼입니다. 사용자는 다양한 카테고리별 최신 글과 인기 글을 탐색할 수 있으며, 회사별 블로그 요약, 게시글 상세 보기, 좋아요 등의 기능을 제공합니다.

---

### Tech Stack

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.2-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-batch)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![pgvector](https://img.shields.io/badge/pgvector-0.8.x-4169E1?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
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

전체 인프라 (AWS)

![TechInsights AWS 아키텍처](./img/architecture/techinsights-aws.drawio.png)

API Server

![API 서버 아키텍처](./img/diagrams/api-server.png)

Batch Server

![Batch 서버 아키텍처](./img/diagrams/batch-server.png)

Crawling Sequence Diagram

![크롤링 시퀀스 다이어그램](./img/diagrams/crawling-sequence.png)

---

## ERD
![ERD](./img/diagrams/erd.png)

---

## 주요 설계

- **DB가 보장하는 데이터 무결성**: `posts.url` UNIQUE(`V016`), 조회 `(post_id, user_or_ip, viewed_date)` UNIQUE(`V018`), 익명 좋아요 `(post_id, anonymous_id)` 부분 UNIQUE(`V019`). 조회 후 삽입(check-then-insert)의 경쟁 상태를 애플리케이션 코드가 아닌 제약조건으로 막고, 쓰기는 `ON CONFLICT DO NOTHING`으로 멱등하게 처리합니다.
- **`aid` 쿠키 기반 익명 식별**: 비로그인 사용자의 조회·좋아요·읽기 이력·추천은 IP가 아니라 서버가 발급한 `aid` 쿠키로 식별합니다. 요청이 `aid` 쿠키를 실제로 들고 오지 않았다면(첫 방문, curl, 봇) 쓰기는 성공 응답만 돌려주고 기록하지 않아 조회수·좋아요 부풀리기를 막습니다.
- **재시도에 안전한 좋아요·북마크**: `PUT`(설정)/`DELETE`(해제) 엔드포인트는 결과 상태를 반환하며 몇 번을 호출해도 같은 결과입니다. 더블 클릭 시 좋아요가 취소되던 토글식 `POST`는 `@Deprecated`로 남겨 두었습니다.
- **리프레시 토큰 패밀리와 멱등 회전**: 로그인 1회가 패밀리(세션) 1개이고, 회전으로 폐기된 토큰의 해시를 패밀리별로 보관해 오래된 토큰이 재사용되면 해당 세션만 폐기합니다. 응답이 유실돼 브라우저가 이전 토큰을 다시 보내도, 유예 시간 안이면 AES-GCM으로 봉인해 둔 같은 후속 토큰을 다시 전달합니다(`V025`, `V032`).
- **HKDF 키 분리**: 하나의 `JWT_SECRET_KEY`에서 JWT 서명, 리프레시 토큰 해싱, 후속 토큰 암호화용 키를 용도별로 따로 유도해, 키를 용도 간에 재사용하지 않습니다(`KeyPurpose`).
- **RFC 7807 에러 계약**: 모든 에러 응답이 `application/problem+json`이며, 클라이언트가 분기에 쓸 안정적인 `code` 필드를 포함합니다. Spring Security의 401/403도 같은 형식입니다.
- **DB 기반 Gemini 일일 쿼터**: 일일(RPD) 사용량을 JVM 메모리가 아닌 `gemini_daily_usage` 테이블에서 원자적 upsert로 차감하므로, 서버를 재시작하거나 인스턴스가 여럿이어도 한도가 유지됩니다(`V020`). 호출 속도 제한과 서킷 브레이커는 Resilience4j가 맡습니다.
- **코사인 거리 + HNSW 벡터 검색**: 임베딩(3072차원)을 `halfvec`로 캐스팅한 `halfvec_cosine_ops` HNSW 인덱스를 두고 `<=>` 연산자로 유사도 검색을 합니다(`V011`, `V021`).
- **Testcontainers와 convention test**: 리포지토리·마이그레이션 테스트는 실제 PostgreSQL + pgvector 컨테이너에서 실제 Flyway 마이그레이션으로 실행합니다. 인라인 FQN 금지 같은 코딩 규칙은 `InlineFqnConventionTest`가 검사합니다.

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

API 서버를 로컬에서 띄우려면 아래 환경 변수가 필요합니다. `application.yml`/`application-local.yml`에 기본값이 없어, 정의되어 있지 않으면 서버가 시작되지 않습니다.

| 변수 | 설명 |
|------|------|
| `DB_PASSWORD` | 로컬 DB 비밀번호. `docker-compose.db.yml`과 서버가 같은 값을 써야 합니다 (서버 기본값: `local_password`) |
| `JWT_SECRET_KEY` | Base64 문자열. 디코딩한 길이가 32바이트(256비트) 이상이어야 합니다. 예: `openssl rand -base64 32` |
| `GEMINI_API_KEY` | Google Gemini API 키 |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth2 로그인용 클라이언트 정보 |

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

커버리지 리포트: `build/reports/jacoco/test/html/index.html`

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
