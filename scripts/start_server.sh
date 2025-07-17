#!/bin/bash
# 이 스크립트는 CodeDeploy에 의해 EC2 인스턴스에서 실행됩니다.
# AWS Secrets Manager를 사용하여 Docker Hub 인증 정보를 안전하게 가져옵니다.

echo "Starting server with image tag: ${IMAGE_TAG}"

# 스크립트 실행 위치로 이동
cd /home/ec2-user/techinsights

# 필요한 변수 설정
AWS_REGION="ap-northeast-2"
SECRET_NAME="techinsights/dockerhub-credentials"

# JSON 파싱을 위한 jq 설치 (없는 경우)
if ! command -v jq &> /dev/null
then
    echo "jq could not be found, installing..."
    sudo yum install -y jq
fi

# AWS Secrets Manager에서 Docker Hub 인증 정보 가져오기
echo "Fetching Docker Hub credentials from AWS Secrets Manager..."
SECRET_JSON=$(aws secretsmanager get-secret-value --secret-id ${SECRET_NAME} --region ${AWS_REGION} --query SecretString --output text)

# SecretString이 비어 있는지 확인
if [ -z "${SECRET_JSON}" ]; then
  echo "Error: Could not retrieve secret from AWS Secrets Manager."
  exit 1
fi

# jq를 사용하여 사용자 이름과 토큰 추출
DOCKERHUB_USERNAME=$(echo ${SECRET_JSON} | jq -r '.username')
DOCKERHUB_TOKEN=$(echo ${SECRET_JSON} | jq -r '.password')

# 추출된 값이 비어 있는지 확인
if [ -z "${DOCKERHUB_USERNAME}" ] || [ -z "${DOCKERHUB_TOKEN}" ]; then
  echo "Error: Username or password not found in the secret."
  exit 1
fi

# Docker Hub에 로그인
echo "Logging in to Docker Hub..."
echo "${DOCKERHUB_TOKEN}" | docker login -u "${DOCKERHUB_USERNAME}" --password-stdin

# docker-compose.prod.yml을 사용하여 애플리케이션 시작
echo "Starting application using docker-compose..."
docker-compose -f docker-compose.prod.yml pull api
docker-compose -f docker-compose.prod.yml up -d --remove-orphans

echo "Server startup script finished."
