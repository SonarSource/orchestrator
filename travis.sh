#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v58 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
  source ~/.local/bin/setup_promote_environment
}
installTravisTools
source ~/.local/bin/installMaven35

cancel_branch_build_with_pr || if [[ $? -eq 1 ]]; then exit 0; fi

export DEPLOY_PULL_REQUEST=true
source regular_mvn_build_deploy_analyze
promote

./check-license-compliance.sh
