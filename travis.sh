#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/master | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

# Do not deploy a SNAPSHOT version but the release version related to this build,
# for example "1.2-build123"
set_maven_build_version $TRAVIS_BUILD_NUMBER

if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  echo 'Build, deploy and analyze commit in master'
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
    -Pcoverage-per-test,deploy-sonarsource \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -B -e -V

elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
  echo 'Build and analyze pull request'
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify \
    -Pcoverage-per-test \
    -Dmaven.test.redirectTestOutputToFile=false \
    -Dsonar.analysis.mode=issues \
    -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
    -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.github.oauth=$GITHUB_TOKEN \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -B -e -V
    
else
  echo 'Build, no deploy, no analysis'
  # Build branch, without any analysis
  mvn verify \
    -Dmaven.test.redirectTestOutputToFile=false \
    -B -e -V
fi

