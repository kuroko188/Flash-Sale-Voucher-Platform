#!/usr/bin/env bash
# Add GitHub credentials to Jenkins for private repo access.
# Usage: GITHUB_TOKEN=ghp_xxx ./scripts/setup-jenkins-github-credential.sh
set -euo pipefail

TOKEN="${GITHUB_TOKEN:-}"
JENKINS_CONTAINER="${JENKINS_CONTAINER:-jenkins}"
CRED_ID="${CRED_ID:-github-token}"
GITHUB_USER="${GITHUB_USER:-kuroko188}"

if [[ -z "${TOKEN}" ]]; then
  echo "Usage: GITHUB_TOKEN=ghp_xxxx ./scripts/setup-jenkins-github-credential.sh"
  echo
  echo "Create token: GitHub -> Settings -> Developer settings -> Personal access tokens"
  echo "Scopes needed: repo (read access to private repositories)"
  exit 1
fi

docker exec "${JENKINS_CONTAINER}" bash -c "cat > /tmp/add-github-cred.groovy <<'GROOVY'
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import hudson.util.Secret
import jenkins.model.Jenkins

def jenkins = Jenkins.getInstance()
def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
def domain = Domain.global()

store.getCredentials(domain).findAll { it.id == '${CRED_ID}' }.each {
  store.removeCredentials(domain, it)
}

def cred = new UsernamePasswordCredentialsImpl(
  CredentialsScope.GLOBAL,
  '${CRED_ID}',
  'GitHub PAT for Flash-Sale-Voucher-Platform',
  '${GITHUB_USER}',
  '${TOKEN}'
)
store.addCredentials(domain, cred)
println 'GitHub credential ${CRED_ID} saved'
GROOVY
"

docker exec "${JENKINS_CONTAINER}" bash -c \
  "sed -i \"s/\\\${CRED_ID}/${CRED_ID}/g; s/\\\${GITHUB_USER}/${GITHUB_USER}/g; s/\\\${TOKEN}/${TOKEN}/g\" /tmp/add-github-cred.groovy"

docker cp "${PWD}/scripts/jenkins/pipeline-job-config.xml" "${JENKINS_CONTAINER}:/var/jenkins_home/jobs/flash-sale-voucher-platform/config.xml"

echo "Done. Re-run Jenkins build: flash-sale-voucher-platform -> Build Now"
