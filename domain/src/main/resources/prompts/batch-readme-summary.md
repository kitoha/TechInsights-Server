# 지시사항

아래 GitHub 레포지토리들의 README를 각각 한국어로 2줄 이내로 요약해주세요.

## 요구사항
- 반드시 한국어로 요약합니다
- 기술 용어는 영어 그대로 사용합니다 (예: "React 기반", "TypeScript 지원", "REST API 제공")
- 각 요약은 2문장 이내의 순수 텍스트(마크다운 없음)여야 합니다
- 각 결과에는 원본 레포지토리의 ID(full_name)를 반드시 포함해야 합니다
- README가 없거나 내용이 불충분한 경우 success=false로 응답하고, batch-readme-schema.json의 error 필드에 실패 이유를 반드시 한국어로 기재합니다 (예: "README 파일이 존재하지 않습니다", "내용이 너무 짧아 요약할 수 없습니다", "설치 방법만 있고 프로젝트 설명이 없습니다")

## 처리할 레포지토리 수: {{repo_count}}개

## Repositories
{{repos}}
