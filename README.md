# Tech Insights - Backend Service

## 프로젝트 개요

Tech Insights는 최신 IT 기술 관련 회사들의 기술 블로그 게시글을 모아 보여주는 플랫폼입니다. 사용자는 다양한 카테고리별 최신 글과 인기 글을 탐색할 수 있으며, 회사별 블로그 요약, 게시글 상세 보기, 댓글 작성, 좋아요 등의 기능을 제공합니다.

## 주요 기능

- 회사별 기술 블로그 요약 및 링크 제공
- 카테고리별 게시글 목록 및 검색 기능
- 인기글, 트렌딩 글 통계 및 차트 제공
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

## Initial Design

### 데이터베이스 ERD

```mermaid
erDiagram
    USER {
        BIGINT id PK "내부 사용자 ID"
        VARCHAR username
        VARCHAR email "대표 이메일"
        VARCHAR profileImageUrl "대표 프로필 이미지"
        VARCHAR role "ADMIN, USER"
        DATETIME createdAt
        DATETIME updatedAt
    }

    USER_AUTH_PROVIDER {
        BIGINT id PK
        BIGINT userId FK
        VARCHAR provider "GOOGLE, KAKAO, NAVER 등"
        VARCHAR providerUserId "Provider별 고유 식별자"
        VARCHAR providerEmail "해당 Provider에서 제공하는 이메일"
        VARCHAR providerProfileImageUrl "해당 Provider에서 제공하는 이미지"
        DATETIME createdAt
        DATETIME updatedAt
    }

    COMPANY {
        BIGINT id PK
        VARCHAR name
        INT totalPostsCount
        INT totalViewCount
        VARCHAR blogUrl
        VARCHAR logoUrl
        DATETIME createdAt
        DATETIME updatedAt
    }

    CATEGORY {
        BIGINT id PK
        VARCHAR name
    }

    TAG {
        BIGINT id PK
        VARCHAR name
    }

    POST {
        BIGINT id PK
        BIGINT companyId FK
        BIGINT categoryId FK
        VARCHAR title
        TEXT content
        VARCHAR authorName
        DATETIME publishedAt
        INT viewCount "실시간 집계(캐시/배치 갱신)"
        INT likeCount "실시간 집계(캐시/배치 갱신)"
        INT shareCount
        DATETIME createdAt
        DATETIME updatedAt
        VARCHAR originalUrl
    }

    POST_TAG {
        BIGINT postId FK
        BIGINT tagId FK
    }

    COMMENT {
        BIGINT id PK
        BIGINT postId FK
        BIGINT userId FK
        BIGINT parentId FK
        TEXT content
        DATETIME createdAt
        DATETIME updatedAt
    }

    POST_VIEW {
        BIGINT id PK
        BIGINT postId FK
        BIGINT userId FK
        DATETIME viewedAt
        VARCHAR sessionOrIp
    }

    POST_LIKE {
        BIGINT id PK
        BIGINT postId FK
        BIGINT userId FK
        DATETIME likedAt
    }

    USER ||--o{ USER_AUTH_PROVIDER : has_provider
    USER ||--o{ COMMENT : writes
    POST ||--o{ COMMENT : has
    COMPANY ||--o{ POST : owns
    CATEGORY ||--o{ POST : categorizes
    POST ||--o{ POST_TAG : post_tag
    TAG ||--o{ POST_TAG : tag_link
    COMMENT ||--o{ COMMENT : reply
    POST ||--o{ POST_VIEW : viewed
    POST ||--o{ POST_LIKE : liked
```

### Design

Home

![Image](https://github.com/user-attachments/assets/d5533bfa-e6cb-46af-9c32-16a3d9b98aa0)