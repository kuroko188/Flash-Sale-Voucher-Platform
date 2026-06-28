#!/usr/bin/env bash
# Create AWS free-tier demo infrastructure via Terraform.
# Prerequisites: aws configure, terraform (brew install terraform)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TF_DIR="${ROOT}/terraform"

echo "==> COST WARNING (demo only — tear down when done):"
echo "    Public IPv4 ~\$0.12/day each. After demo:"
echo "    ./scripts/teardown-aws-free-tier.sh"
echo

if [[ ! -f "${TF_DIR}/terraform.tfvars" ]]; then
  echo "==> Creating ${TF_DIR}/terraform.tfvars from example"
  cp "${TF_DIR}/terraform.tfvars.example" "${TF_DIR}/terraform.tfvars"
  echo "    Edit rds_password in terraform/terraform.tfvars if needed"
fi

cd "${TF_DIR}"
terraform init -input=false
terraform apply

echo
echo "==> Outputs:"
terraform output

echo
echo "==> NEXT STEPS:"
echo "1. Import schema:"
terraform output -raw import_schema_commands
echo
echo "2. Build & push image (or Jenkins SKIP_DEPLOY=false):"
echo "   ECR=\$(terraform output -raw ecr_repository_url)"
echo "   aws ecr get-login-password --region \${AWS_REGION:-us-east-1} | docker login --username AWS --password-stdin \$(echo \$ECR | cut -d/ -f1)"
echo "   docker build -t \$ECR:latest . && docker push \$ECR:latest"
echo
echo "3. Deploy new task revision:"
echo "   ${ROOT}/scripts/deploy-ecs-ec2-free-tier.sh --from-terraform --image \$ECR:latest"
echo
echo "4. Open app:"
echo "   \$(terraform output -raw app_url)/login.html"
echo
echo "5. Tear down:"
echo "   ${ROOT}/scripts/teardown-aws-free-tier.sh"
