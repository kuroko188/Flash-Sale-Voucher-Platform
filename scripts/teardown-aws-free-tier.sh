#!/usr/bin/env bash
# Tear down Terraform-managed AWS demo resources.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TF_DIR="${ROOT}/terraform"

echo "==> This will run: terraform destroy"
echo "    Press Ctrl+C within 5 seconds to cancel..."
sleep 5

cd "${TF_DIR}"
terraform destroy

echo
echo "==> Done. Verify Billing -> Bills in 24h for any remaining charges."
