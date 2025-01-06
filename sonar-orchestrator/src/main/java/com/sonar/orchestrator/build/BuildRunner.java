/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Server;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class BuildRunner {

  public static final String SONAR_HOST_URL = "sonar.host.url";
  private final Configuration config;

  public BuildRunner(Configuration config) {
    this.config = config;
  }

  public BuildResult runQuietly(@Nullable Server server, Build<?> build) {
    return build.execute(config, adjustProperties(server, build));
  }

  public BuildResult run(@Nullable Server server, Build<?> build) {
    BuildResult result = runQuietly(server, build);
    if (!result.isSuccess()) {
      throw new BuildFailureException(build, result);
    }
    return result;
  }

  Map<String, String> adjustProperties(@Nullable Server server, Build<?> build) {
    Map<String, String> adjustedProperties = new HashMap<>();
    if (!(build instanceof ScannerForMSBuild) || !build.arguments().contains("end")) {
      if (server != null) {
        adjustedProperties.put(SONAR_HOST_URL, server.getUrl());
      }
      adjustedProperties.put("sonar.scm.disabled", "true");
      adjustedProperties.put("sonar.branch.autoconfig.disabled", "true");
    }
    // build properties override predefined properties
    adjustedProperties.putAll(build.getProperties());

    return adjustedProperties;
  }

}
