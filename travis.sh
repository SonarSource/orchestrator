#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v19 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

# We need some private jars like oracle
installTravisTools

mvn verify -B -e -V -Dmaven.test.redirectTestOutputToFile=false
