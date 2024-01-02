/*
 * Orchestrator
 * Copyright (C) 2011-2024 SonarSource SA
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
import com.sonar.orchestrator.coverage.JaCoCoArgumentsBuilder;
import com.sonar.orchestrator.util.CommandExecutor;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractBuildExecutor<T extends Build<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractBuildExecutor.class);

  final BuildResult execute(T build, Configuration config, Map<String, String> adjustedProperties) {
    return execute(build, config, adjustedProperties, CommandExecutor.create());
  }

  abstract BuildResult execute(T build, Configuration config, Map<String, String> adjustedProperties, CommandExecutor create);

  /**
   * Append JaCoCo agent JVM arguments to a given environment variable (SONAR_RUNNER_OPTS, MAVEN_OPTS, ...)
   * @param optsVariableName The name of the environment variable to append to
   */
  static void appendCoverageArgumentToOpts(Map<String, String> environmentVariables, Configuration config, String optsVariableName) {
    String jaCoCoArgument = JaCoCoArgumentsBuilder.getJaCoCoArgument(config);
    if (jaCoCoArgument != null) {
      if (environmentVariables.containsKey(optsVariableName)) {
        String opts = environmentVariables.get(optsVariableName) + " " + jaCoCoArgument;
        environmentVariables.put(optsVariableName, opts);
      } else {
        environmentVariables.put(optsVariableName, jaCoCoArgument);
      }
    }
    if (environmentVariables.containsKey(optsVariableName)) {
      LOG.info("{}: {}", optsVariableName, environmentVariables.get(optsVariableName));
    }
  }

}
