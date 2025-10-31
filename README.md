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

## 시스템 아키텍처

```mermaid
graph TB
    subgraph "클라이언트"
        Client[Web Browser]
    end

    subgraph "인프라 레이어"
        Nginx[Nginx<br/>리버스 프록시 & SSL]
    end

    subgraph "애플리케이션 레이어"
        subgraph "API 모듈"
            Controller[Controllers<br/>post, search, recommend<br/>company, category]
            AID[AID Manager<br/>익명 사용자 추적]
        end

        subgraph "Domain 모듈"
            Service[Domain Services<br/>PostService, SearchService<br/>RecommendationService]
            Repository[Repositories<br/>JPA + Querydsl]
            Embedding[Embedding Service<br/>벡터 임베딩]
            Summarizer[Article Summarizer<br/>AI 요약]
        end

        subgraph "Batch 모듈"
            FeedParser[RSS Feed Parser<br/>Atom/RSS]
            Crawler[Web Crawler<br/>컨텐츠 추출]
            BatchProcessor[Batch Processor<br/>요약 & 임베딩]
        end
    end

    subgraph "데이터 레이어"
        PostgreSQL[(PostgreSQL<br/>관계형 데이터<br/>벡터 검색)]
    end

    subgraph "외부 API"
        Gemini[Google Gemini API<br/>요약 & 임베딩]
        RSS[기업 기술블로그<br/>RSS Feeds]
    end

    Client -->|HTTPS| Nginx
    Nginx -->|Proxy| Controller
    Controller --> AID
    Controller --> Service
    Service --> Repository
    Service --> Embedding
    Service --> Summarizer
    Repository --> PostgreSQL

    FeedParser -->|스케줄링| RSS
    RSS -->|XML/Atom| FeedParser
    FeedParser --> Crawler
    Crawler --> BatchProcessor
    BatchProcessor --> Summarizer
    BatchProcessor --> Embedding
    BatchProcessor --> Repository

    Embedding -->|API 호출| Gemini
    Summarizer -->|API 호출| Gemini

    style Client fill:#e1f5ff
    style Nginx fill:#ffe1e1
    style PostgreSQL fill:#e1ffe1
    style Gemini fill:#fff4e1
    style RSS fill:#f0e1ff
```

## 기술 스택

- **Language:** Kotlin
- **Backend:** Spring Boot, Spring Batch, Spring Data JPA
- **AI:** Google Gemini API (Embedding & Vector Search)
- **Build Tool:** Gradle
- **Infra:** Docker, Docker Compose, Nginx
- **CI/CD:** GitHub Actions, AWS CodeDeploy

## 개발 환경

- JDK 21
- Gradle 빌드 시스템
- Docker (개발/배포용)

## 접속 링크

https://www.techinsights.shop/

## Preview

| 검색 기능                                                            | 세부 페이지                                                                | 다크모드                                                              |
|------------------------------------------------------------------|-----------------------------------------------------------------------|-------------------------------------------------------------------|
| <img src="./img/gif/Search_Test.gif" alt="검색 기능 데모" width="300"> | <img src="./img/gif/DetailView_Test.gif" alt="세부 페이지 데모" width="300"> | <img src="./img/gif/DarkMode_Test.gif" alt="다크모드 데모" width="300"> |

### 데이터베이스 ERD

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
    }

    post_categories {
        Long post_id FK "게시물 ID"
        String category "카테고리"
    }

    PostEmbedding {
        Long postId PK "게시물 ID (FK)"
        String companyName "회사명"
        String categories "카테고리"
        String content "내용"
        FloatArray embeddingVector "임베딩 벡터"
    }

    PostView {
        Long id PK
        Long postId FK "게시물 ID"
        String userOrIp "사용자 또는 IP"
        LocalDate viewedDate "조회일"
    }

    AnonymousUserReadHistory {
        Long id PK
        String anonymousId "익명 사용자 ID"
        Long postId FK "게시물 ID"
        LocalDateTime readAt "읽은 시간"
    }

    Company ||--o{ Post : "owns"
    Post }|..|| PostEmbedding : "has one"
    Post ||--o{ PostView : "has many"
    Post ||--o{ AnonymousUserReadHistory : "has many"
    Post ||--o{ post_categories : "has many"
```

### Initial Design

Home

![Image](https://github.com/user-attachments/assets/d5533bfa-e6cb-46af-9c32-16a3d9b98aa0)

