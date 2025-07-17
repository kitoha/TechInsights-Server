#!/bin/bash

echo "Navigating to the application directory..."
cd /home/ec2-user/techinsights

echo "Starting application using the pre-configured docker-compose file..."
# docker-compose.prod.yml 파일에는 이미 CI 단계에서 완성된 이미지 이름이 들어있습니다.
# (예: image: kitoha/tech-insights-api:202507171200)

# 1. 최신 버전의 이미지를 Docker Hub에서 받아옵니다.
docker-compose -f docker-compose.prod.yml pull

# 2. 컨테이너를 백그라운드에서 실행합니다.
# --remove-orphans: docker-compose 파일에서 제거된 서비스의 컨테이너를 삭제합니다.
docker-compose -f docker-compose.prod.yml up -d --remove-orphans

# 3. 사용하지 않는 이전 버전의 Docker 이미지를 정리하여 디스크 공간을 확보합니다.
docker image prune -f

echo "Server startup script finished successfully."
