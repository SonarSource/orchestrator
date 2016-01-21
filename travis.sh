#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v21 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

# We need some private jars like oracle
installTravisTools

if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  echo 'Build and analyze commit in master'
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
    -Pcoverage-per-test -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -B -e -V

elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ "$TRAVIS_SECURE_ENV_VARS" == "true" ]; then
  echo 'Build and analyze pull request'
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify \
    -Pcoverage-per-test -Dmaven.test.redirectTestOutputToFile=false -B -e -V \
    -Dsonar.analysis.mode=issues \
    -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
    -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.github.oauth=$GITHUB_TOKEN \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN
    
else
  echo 'Build, no analysis'
  # Build branch, without any analysis
  mvn verify -Dmaven.test.redirectTestOutputToFile=false -B -e -V
fi
