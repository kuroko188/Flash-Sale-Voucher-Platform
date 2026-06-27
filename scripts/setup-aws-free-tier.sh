#!/usr/bin/env bash
# One-time AWS Free Tier setup helper (12-month free tier for new accounts).
# Run locally after: aws configure
set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
KEY_NAME="${KEY_NAME:-flash-sale-key}"
CLUSTER="${ECS_CLUSTER:-flash-sale-cluster}"
SERVICE="${ECS_SERVICE:-flash-sale-service}"
ECR_REPO="${ECR_REPOSITORY:-flash-sale-voucher-platform}"
DB_ID="${RDS_INSTANCE_ID:-flash-sale-mysql}"
DB_USER="${RDS_USERNAME:-admin}"
DB_PASS="${RDS_PASSWORD:-FlashSale123!}"
SG_NAME="flash-sale-free-tier-sg"

echo "==> Region: ${REGION}"
echo "==> This script uses FREE TIER eligible resources only:"
echo "    - ECR (500MB/month free)"
echo "    - EC2 t3.micro (750 hrs/month, 12 months)"
echo "    - RDS db.t3.micro MySQL (750 hrs/month, 12 months)"
echo "    - Redis runs as container on EC2 (NOT ElastiCache)"
echo "    - ECS on EC2 (NOT Fargate)"
echo

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "==> AWS Account: ${ACCOUNT_ID}"

echo "==> [1/6] Create ECR repository"
aws ecr describe-repositories --repository-names "${ECR_REPO}" --region "${REGION}" >/dev/null 2>&1 || \
  aws ecr create-repository --repository-name "${ECR_REPO}" --region "${REGION}"

echo "==> [2/6] Create CloudWatch log group"
aws logs create-log-group --log-group-name /ecs/flash-sale-voucher-platform --region "${REGION}" 2>/dev/null || true

echo "==> [3/6] Create security group"
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text --region "${REGION}")
SG_ID=$(aws ec2 describe-security-groups --filters Name=group-name,Values="${SG_NAME}" --query 'SecurityGroups[0].GroupId' --output text --region "${REGION}" 2>/dev/null || echo "None")
if [[ "${SG_ID}" == "None" || -z "${SG_ID}" ]]; then
  SG_ID=$(aws ec2 create-security-group --group-name "${SG_NAME}" --description "Flash sale free tier" --vpc-id "${VPC_ID}" --region "${REGION}" --query GroupId --output text)
  aws ec2 authorize-security-group-ingress --group-id "${SG_ID}" --protocol tcp --port 22 --cidr 0.0.0.0/0 --region "${REGION}"
  aws ec2 authorize-security-group-ingress --group-id "${SG_ID}" --protocol tcp --port 8081 --cidr 0.0.0.0/0 --region "${REGION}"
fi
echo "    Security Group: ${SG_ID}"

echo "==> [4/6] Create RDS MySQL (db.t3.micro free tier)"
if ! aws rds describe-db-instances --db-instance-identifier "${DB_ID}" --region "${REGION}" >/dev/null 2>&1; then
  aws rds create-db-instance \
    --db-instance-identifier "${DB_ID}" \
    --db-instance-class db.t3.micro \
    --engine mysql \
    --engine-version 5.7.44 \
    --master-username "${DB_USER}" \
    --master-user-password "${DB_PASS}" \
    --allocated-storage 20 \
    --db-name hmdp \
    --backup-retention-period 0 \
    --no-multi-az \
    --publicly-accessible \
    --vpc-security-group-ids "${SG_ID}" \
    --region "${REGION}"
  echo "    RDS creating... wait 5-10 minutes, then run:"
  echo "    aws rds describe-db-instances --db-instance-identifier ${DB_ID} --query 'DBInstances[0].Endpoint.Address' --output text --region ${REGION}"
else
  echo "    RDS already exists: ${DB_ID}"
fi

echo "==> [5/6] Create ECS cluster (EC2 launch type)"
aws ecs describe-clusters --clusters "${CLUSTER}" --region "${REGION}" --query 'clusters[0].status' --output text 2>/dev/null | grep -q ACTIVE || \
  aws ecs create-cluster --cluster-name "${CLUSTER}" --region "${REGION}"

echo "==> [6/6] Launch EC2 t3.micro with ECS-Optimized AMI (free tier)"
SUBNET_ID=$(aws ec2 describe-subnets --filters Name=default-for-az,Values=true --query 'Subnets[0].SubnetId' --output text --region "${REGION}")
AMI=$(aws ssm get-parameters --names /aws/service/ecs/optimized-ami/amazon-linux-2/recommended/image_id \
  --query 'Parameters[0].Value' --output text --region "${REGION}")

INSTANCE_PROFILE="${INSTANCE_PROFILE:-ecsInstanceRole}"
if ! aws iam get-instance-profile --instance-profile-name "${INSTANCE_PROFILE}" >/dev/null 2>&1; then
  echo "    WARNING: IAM instance profile '${INSTANCE_PROFILE}' not found."
  echo "    Create it in AWS Console: IAM -> Roles -> Create ecsInstanceRole for EC2 + ECS"
fi

EXISTING=$(aws ec2 describe-instances \
  --filters Name=tag:Name,Values=flash-sale-ecs-host Name=instance-state-name,Values=running,pending \
  --query 'Reservations[0].Instances[0].InstanceId' --output text --region "${REGION}" 2>/dev/null || echo "None")

if [[ "${EXISTING}" == "None" || -z "${EXISTING}" ]]; then
  USER_DATA=$(cat <<EOF
#!/bin/bash
echo ECS_CLUSTER=${CLUSTER} >> /etc/ecs/ecs.config
echo ECS_ENABLE_CONTAINER_METADATA=true >> /etc/ecs/ecs.config
EOF
)
  INSTANCE_ID=$(aws ec2 run-instances \
    --image-id "${AMI}" \
    --instance-type t3.micro \
    --count 1 \
    --subnet-id "${SUBNET_ID}" \
    --security-group-ids "${SG_ID}" \
    --iam-instance-profile Name="${INSTANCE_PROFILE}" \
    --user-data "${USER_DATA}" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=flash-sale-ecs-host}]" \
    --region "${REGION}" \
    --query 'Instances[0].InstanceId' --output text)
  echo "    EC2 instance launched: ${INSTANCE_ID}"
else
  echo "    EC2 already running: ${EXISTING}"
fi

echo
echo "==> NEXT STEPS (manual):"
echo "1. Wait for RDS endpoint, then import schema:"
echo "   RDS_HOST=\$(aws rds describe-db-instances --db-instance-identifier ${DB_ID} --query 'DBInstances[0].Endpoint.Address' --output text --region ${REGION})"
echo "   mysql -h \$RDS_HOST -u ${DB_USER} -p hmdp < hmdp.sql"
echo "   mysql -h \$RDS_HOST -u ${DB_USER} -p hmdp < scripts/seed-english-data.sql"
echo
echo "2. Edit aws/ecs-task-definition-ec2-free-tier.json:"
echo "   - Replace ACCOUNT_ID with ${ACCOUNT_ID}"
echo "   - Replace RDS_ENDPOINT with RDS host"
echo "   - Replace CHANGE_ME with ${DB_PASS}"
echo
echo "3. Register task definition and create ECS service (EC2 launch type) in AWS Console"
echo "   or run Jenkins pipeline with DEPLOY_TARGET=free-tier-ec2"
echo
echo "4. Open app: http://<EC2_PUBLIC_IP>:8081"
echo
echo "==> Estimated monthly cost on free tier: \$0 (within 12-month limits)"
echo "    Avoid: Fargate, ElastiCache, larger instance types"
