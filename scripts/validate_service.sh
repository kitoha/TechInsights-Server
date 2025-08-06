#!/bin/bash

for i in {1..10}; do
  response=$(curl --write-out %{http_code} --silent --output /dev/null http://localhost/actuator/health)

  if [[ "$response" -ge 200 && "$response" -lt 400 ]]; then
    echo "Service validation successful with status code $response."
    exit 0 # 성공 시 0을 반환하여 CodeDeploy에 성공을 알립니다.
  fi
  
  echo "Validation attempt $i failed with status code $response. Retrying in 5 seconds..."
  sleep 5
done

echo "Service validation failed after multiple attempts."
exit 1 # 최종 실패 시 1을 반환하여 CodeDeploy에 실패를 알리고 롤백을 트리거합니다.
