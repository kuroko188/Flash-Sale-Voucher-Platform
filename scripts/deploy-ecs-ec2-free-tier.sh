#!/usr/bin/env bash
set -euo pipefail

REGION=""
CLUSTER=""
SERVICE=""
TASK_FAMILY=""
IMAGE=""

usage() {
  echo "Usage: $0 --region REGION --cluster CLUSTER --service SERVICE --task-family FAMILY --image IMAGE_URI"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --region) REGION="$2"; shift 2 ;;
    --cluster) CLUSTER="$2"; shift 2 ;;
    --service) SERVICE="$2"; shift 2 ;;
    --task-family) TASK_FAMILY="$2"; shift 2 ;;
    --image) IMAGE="$2"; shift 2 ;;
    *) usage ;;
  esac
done

[[ -n "$REGION" && -n "$CLUSTER" && -n "$SERVICE" && -n "$TASK_FAMILY" && -n "$IMAGE" ]] || usage

TASK_DEF_FILE="$(mktemp)"
trap 'rm -f "$TASK_DEF_FILE"' EXIT

python3 - <<PY
import json
from pathlib import Path

template = json.loads(Path("aws/ecs-task-definition-ec2-free-tier.json").read_text())
template["containerDefinitions"][1]["image"] = "${IMAGE}"
Path("${TASK_DEF_FILE}").write_text(json.dumps(template))
PY

NEW_TASK_ARN=$(aws ecs register-task-definition \
  --region "$REGION" \
  --cli-input-json "file://${TASK_DEF_FILE}" \
  --query 'taskDefinition.taskDefinitionArn' \
  --output text)

if aws ecs describe-services --cluster "$CLUSTER" --services "$SERVICE" --region "$REGION" \
  --query 'services[0].status' --output text 2>/dev/null | grep -q ACTIVE; then
  aws ecs update-service \
    --region "$REGION" \
    --cluster "$CLUSTER" \
    --service "$SERVICE" \
    --task-definition "$NEW_TASK_ARN" \
    --force-new-deployment \
    --query 'service.serviceName' \
    --output text
  aws ecs wait services-stable --region "$REGION" --cluster "$CLUSTER" --services "$SERVICE"
else
  echo "Service ${SERVICE} not found. Create it once in AWS Console:"
  echo "  ECS -> ${CLUSTER} -> Create Service -> EC2 launch type -> task ${TASK_FAMILY}"
  echo "  Registered task definition: ${NEW_TASK_ARN}"
  exit 0
fi

echo "Deployed ${IMAGE} to ${CLUSTER}/${SERVICE} (EC2 free tier)"
