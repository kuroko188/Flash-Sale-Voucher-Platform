#!/usr/bin/env bash
# Local Jenkins CI: mount project dir, no GitHub token needed.
set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-/Users/wangshengbin/work/hmdp}"
JENKINS_NAME="${JENKINS_NAME:-jenkins}"
JENKINS_PORT="${JENKINS_PORT:-8080}"

echo "==> Project: ${PROJECT_DIR}"
echo "==> Recreating Jenkins with local workspace mount"

docker rm -f "${JENKINS_NAME}" 2>/dev/null || true

docker run -d \
  --name "${JENKINS_NAME}" \
  -p "${JENKINS_PORT}:8080" \
  -p 50000:50000 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "${PROJECT_DIR}:/workspace/hmdp:rw" \
  jenkins/jenkins:lts-jdk17

echo "==> Waiting for Jenkins to start..."
sleep 20

echo "==> Installing Maven, Git, Docker CLI"
docker exec -u root "${JENKINS_NAME}" bash -c \
  'apt-get update -qq && apt-get install -y -qq maven git docker.io python3 curl >/dev/null'
docker exec -u root "${JENKINS_NAME}" chmod 666 /var/run/docker.sock
docker exec "${JENKINS_NAME}" git config --global --add safe.directory /workspace/hmdp

echo "==> Installing Jenkins plugins"
docker exec -u root "${JENKINS_NAME}" jenkins-plugin-cli \
  --plugins git workflow-aggregator workflow-job pipeline-groovy-lib jacoco junit >/dev/null

echo "==> Applying init scripts"
docker exec "${JENKINS_NAME}" mkdir -p /var/jenkins_home/init.groovy.d
for f in 01-skip-wizard 02-create-admin 99-reset-password; do
  docker cp "${PROJECT_DIR}/scripts/jenkins/${f}.groovy" \
    "${JENKINS_NAME}:/var/jenkins_home/init.groovy.d/${f}.groovy"
done

docker restart "${JENKINS_NAME}"
sleep 25

echo "==> Creating pipeline job (local file:// SCM)"
docker exec "${JENKINS_NAME}" mkdir -p /var/jenkins_home/jobs/flash-sale-voucher-platform
docker cp "${PROJECT_DIR}/scripts/jenkins/local-pipeline-job-config.xml" \
  "${JENKINS_NAME}:/var/jenkins_home/jobs/flash-sale-voucher-platform/config.xml"

echo
echo "==> Jenkins ready"
echo "    URL:      http://localhost:${JENKINS_PORT}"
echo "    User:     admin"
echo "    Password: admin123"
echo "    Job:      flash-sale-voucher-platform"
echo "    Source:   file:///workspace/hmdp (local, no GitHub token)"
echo
echo "Open Jenkins and click Build Now."
