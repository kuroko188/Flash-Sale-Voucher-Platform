#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TF_DIR="${ROOT}/terraform"
REGION=""
CLUSTER=""
SERVICE=""
TASK_FAMILY=""
IMAGE=""
FROM_TERRAFORM=false

usage() {
  echo "Usage: $0 --image IMAGE_URI [--region REGION] [--cluster CLUSTER] [--service SERVICE] [--task-family FAMILY]"
  echo "       $0 --from-terraform --image IMAGE_URI"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --region) REGION="$2"; shift 2 ;;
    --cluster) CLUSTER="$2"; shift 2 ;;
    --service) SERVICE="$2"; shift 2 ;;
    --task-family) TASK_FAMILY="$2"; shift 2 ;;
    --image) IMAGE="$2"; shift 2 ;;
    --from-terraform) FROM_TERRAFORM=true; shift ;;
    *) usage ;;
  esac
done

[[ -n "$IMAGE" ]] || usage

if $FROM_TERRAFORM || [[ -f "${TF_DIR}/terraform.tfstate" ]]; then
  if [[ -f "${TF_DIR}/terraform.tfstate" ]] && command -v terraform >/dev/null 2>&1; then
    cd "${TF_DIR}"
    REGION="${REGION:-$(terraform output -raw aws_region)}"
    CLUSTER="${CLUSTER:-$(terraform output -raw ecs_cluster_name)}"
    SERVICE="${SERVICE:-$(terraform output -raw ecs_service_name)}"
    TASK_FAMILY="${TASK_FAMILY:-$(terraform output -raw ecs_task_family)}"
    export DEPLOY_RDS_ENDPOINT="$(terraform output -raw rds_endpoint)"
    export DEPLOY_RDS_USER="$(terraform output -raw rds_username)"
    export DEPLOY_RDS_PASS="$(terraform output -raw rds_password)"
    cd "${ROOT}"
  else
    REGION="${REGION:-${AWS_REGION:-us-east-1}}"
    export DEPLOY_RDS_ENDPOINT="$(aws rds describe-db-instances \
      --db-instance-identifier flash-sale-mysql \
      --region "${REGION}" \
      --query 'DBInstances[0].Endpoint.Address' \
      --output text 2>/dev/null || true)"
    export DEPLOY_RDS_USER="${RDS_USERNAME:-admin}"
    export DEPLOY_RDS_PASS="${RDS_PASSWORD:-}"
  fi
  export DEPLOY_FROM_TERRAFORM=true
else
  export DEPLOY_FROM_TERRAFORM=false
fi

[[ -n "$REGION" && -n "$CLUSTER" && -n "$SERVICE" && -n "$TASK_FAMILY" ]] || usage

TASK_DEF_FILE="$(mktemp)"
trap 'rm -f "$TASK_DEF_FILE"' EXIT

export DEPLOY_IMAGE="${IMAGE}"
export DEPLOY_REGION="${REGION}"
export DEPLOY_TASK_DEF_FILE="${TASK_DEF_FILE}"

python3 - <<'PY'
import json
import os
from pathlib import Path

template = json.loads(Path("aws/ecs-task-definition-ec2-free-tier.json").read_text())
app = template["containerDefinitions"][1]
app["image"] = os.environ["DEPLOY_IMAGE"]

if os.environ.get("DEPLOY_FROM_TERRAFORM") == "true":
    rds = os.environ.get("DEPLOY_RDS_ENDPOINT", "")
    rds_user = os.environ.get("DEPLOY_RDS_USER", "admin")
    rds_pass = os.environ.get("DEPLOY_RDS_PASS", "")
    for env in app["environment"]:
        if env["name"] == "SPRING_DATASOURCE_URL" and rds:
            env["value"] = f"jdbc:mysql://{rds}:3306/hmdp?useSSL=false&serverTimezone=UTC"
        elif env["name"] == "SPRING_DATASOURCE_USERNAME":
            env["value"] = rds_user
        elif env["name"] == "SPRING_DATASOURCE_PASSWORD" and rds_pass:
            env["value"] = rds_pass
    app["logConfiguration"]["options"]["awslogs-region"] = os.environ["DEPLOY_REGION"]

Path(os.environ["DEPLOY_TASK_DEF_FILE"]).write_text(json.dumps(template))
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
  echo "Service ${SERVICE} not found. Run: cd terraform && terraform apply"
  echo "Registered task definition: ${NEW_TASK_ARN}"
  exit 0
fi

echo "Deployed ${IMAGE} to ${CLUSTER}/${SERVICE} (EC2 free tier)"
