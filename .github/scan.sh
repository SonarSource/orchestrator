#!/bin/bash
set -euxo pipefail

trap 'echo "Script exited with code $?"' EXIT

export GIT_SHA1=${GITHUB_SHA}
export GITHUB_BASE_BRANCH=${GITHUB_BASE_REF:-}
export GITHUB_BRANCH=${GITHUB_REF_NAME}
export GITHUB_REPO=${GITHUB_REPOSITORY}
export PULL_REQUEST=""
if [[ "${GITHUB_EVENT_NAME:-}" == "pull_request" ]]; then
  if command -v jq >/dev/null 2>&1 && [[ -f "${GITHUB_EVENT_PATH:-}" ]]; then
    export PULL_REQUEST="$(jq --raw-output .pull_request.number "$GITHUB_EVENT_PATH")"
  fi
fi
export SONAR_HOST_URL=${SONAR_HOST_URL:-https://sonarcloud.io}
export PROJECT_KEY=${PROJECT_KEY:-SonarSource_orchestrator}
export SONAR_TOKEN=${SONAR_TOKEN:-}

echo "[DEBUG] GIT_SHA1: ${GIT_SHA1}"
echo "[DEBUG] GITHUB_BASE_BRANCH: ${GITHUB_BASE_BRANCH}"
echo "[DEBUG] GITHUB_BRANCH: ${GITHUB_BRANCH}"
echo "[DEBUG] GITHUB_REPO: ${GITHUB_REPO}"
echo "[DEBUG] PULL_REQUEST: ${PULL_REQUEST}"
echo "[DEBUG] SONAR_HOST_URL: ${SONAR_HOST_URL}"
echo "[DEBUG] PROJECT_KEY: ${PROJECT_KEY}"


echo "[DEBUG] Entering main conditional: PULL_REQUEST='${PULL_REQUEST}', GITHUB_BRANCH='${GITHUB_BRANCH}'"
if [[ "${PULL_REQUEST}" ]] || [[ "${GITHUB_BRANCH}" == "master" ]]; then
  scanner_params=()

  if [[ "${GITHUB_BASE_BRANCH}" ]]; then
    git fetch origin "${GITHUB_BASE_BRANCH}"
  fi

  if [[ "${PULL_REQUEST}" ]]; then
    scanner_params+=("-Dsonar.analysis.prNumber=${PULL_REQUEST}")
  fi

  scanner_params+=(
    "-Dsonar.host.url=${SONAR_HOST_URL}"
    "-Dsonar.token=${SONAR_TOKEN}"
    "-Dsonar.qualitygate.wait=false"
    "-Dsonar.analysis.pipeline=${GITHUB_RUN_NUMBER}"
    "-Dsonar.analysis.repository=${GITHUB_REPO}"
    "-Dsonar.analysis.sha1=${GIT_SHA1}"
    "-Dsonar.organization=sonarsource"
    "-Dsonar.projectKey=${PROJECT_KEY}"
    "-Dsonar.java.binaries=**/target/**"
    "-Dsonar.coverage.jacoco.xmlReportPaths=**/build/jacoco-coverage.xml"
    "-Dsonar.sources=."
    "-Dsonar.exclusions=**/src/test/**,**/tests/**"
    "-Dsonar.tests=."
    "-Dsonar.test.inclusions=**/src/test/**,**/tests/**"
  )

  echo "[DEBUG] Running sonar-scanner with params: ${scanner_params[*]}"
  sonar-scanner "${scanner_params[@]}"
else
  echo "[DEBUG] Skipping scan: neither PULL_REQUEST nor GITHUB_BRANCH=master."
fi