#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v33 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis
. installJDK8

 . set_maven_build_version $TRAVIS_BUILD_NUMBER
    mvn deploy \
      -Pdeploy-sonarsource \
      -B -e -V $*
