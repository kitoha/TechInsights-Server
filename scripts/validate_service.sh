#!/bin/bash
# 이 스크립트는 서비스가 성공적으로 시작되었는지 확인합니다.

# Nginx가 80번 포트에서 정상적으로 응답하는지 최대 1분간 확인합니다.
# 5초 간격으로 12번 재시도합니다.
for i in {1..12}; do
  # 로컬호스트의 80번 포트로 요청을 보내 HTTP 상태 코드를 확인합니다.
  # Nginx가 정상적으로 실행되고 api 컨테이너로 프록시가 연결되었다면 200 또는 다른 유효한 코드를 반환할 것입니다.
  # 간단한 확인을 위해 200 OK를 기대하거나, 404 Not Found도 Nginx가 살아있다는 의미일 수 있습니다.
  # 여기서는 /api/health 와 같은 헬스 체크 엔드포인트를 호출하는 것을 가정합니다.
  response=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost/api/health)
  
  # HTTP 응답 코드가 200번대 또는 300번대이면 성공으로 간주합니다.
  if [[ "$response" -ge 200 && "$response" -lt 400 ]]; then
    echo "Service validation successful with status code $response."
    exit 0 # 성공 시 0을 반환하여 CodeDeploy에 성공을 알립니다.
  fi
  
  echo "Validation attempt $i failed with status code $response. Retrying in 5 seconds..."
  sleep 5
done

echo "Service validation failed after multiple attempts."
exit 1 # 최종 실패 시 1을 반환하여 CodeDeploy에 실패를 알리고 롤백을 트리거합니다.
