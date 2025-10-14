# Tech Insights - Backend Service

## 프로젝트 개요

Tech Insights는 최신 IT 기술 관련 회사들의 기술 블로그 게시글을 모아 보여주는 플랫폼입니다. 사용자는 다양한 카테고리별 최신 글과 인기 글을 탐색할 수 있으며, 회사별 블로그 요약, 게시글 상세 보기, 댓글 작성, 좋아요 등의 기능을 제공합니다.

## 주요 기능

- 회사별 기술 블로그 요약 및 링크 제공
- 카테고리별 게시글 목록 및 검색 기능
- 인기글, AI 추천 글 통계 및 차트 제공
- 게시글 상세 보기 및 댓글 기능
- 사용자 인증 및 권한 관리 (관리자, 일반 사용자 구분)

## 기술 스택

- 언어: Kotlin
- 프레임워크: Spring Boot
- 데이터베이스: MySQL 또는 PostgreSQL
- API 문서화: Swagger/OpenAPI
- 보안: Spring Security (인증은 OAUTH2, 인가는 JWT, RefreshToken도 추가.)

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



### Design

Home

![Image](https://github.com/user-attachments/assets/d5533bfa-e6cb-46af-9c32-16a3d9b98aa0)

