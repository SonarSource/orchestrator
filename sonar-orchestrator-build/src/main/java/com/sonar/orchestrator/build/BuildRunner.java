/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.Locators;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class BuildRunner {

  public static final String SONAR_HOST_URL = "sonar.host.url";
  private final Configuration config;
  private final Locators locators;

  public BuildRunner(Configuration config, Locators locators) {
    this.config = config;
    this.locators = locators;
  }

  public BuildResult runQuietly(@Nullable String serverUrl, Build<?> build) {
    return build.execute(config, locators, adjustProperties(serverUrl, build));
  }

  public BuildResult run(@Nullable String serverUrl, Build<?> build) {
    BuildResult result = runQuietly(serverUrl, build);
    if (!result.isSuccess()) {
      throw new BuildFailureException(build, result);
    }
    return result;
  }

  Map<String, String> adjustProperties(@Nullable String serverUrl, Build<?> build) {
    Map<String, String> adjustedProperties = new HashMap<>();
    if (!(build instanceof ScannerForMSBuild) || !build.arguments().contains("end")) {
      if (serverUrl != null) {
        adjustedProperties.put(SONAR_HOST_URL, serverUrl);
      }
      adjustedProperties.put("sonar.scm.disabled", "true");
      adjustedProperties.put("sonar.branch.autoconfig.disabled", "true");
    }
    // build properties override predefined properties
    adjustedProperties.putAll(build.getProperties());

    return adjustedProperties;
  }

}
