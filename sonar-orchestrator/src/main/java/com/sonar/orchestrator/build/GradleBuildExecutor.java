/*
 * Orchestrator
 * Copyright (C) 2011-2023 SonarSource SA
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
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import java.io.File;
import java.util.Map;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;

public class GradleBuildExecutor extends AbstractBuildExecutor<GradleBuild> {

  private final Os os;

  GradleBuildExecutor() {
    this(new Os());
  }

  // Visible for testing
  GradleBuildExecutor(Os os) {
    this.os = os;
  }

  @Override
  BuildResult execute(GradleBuild build, Configuration config, Map<String, String> adjustedProperties, CommandExecutor commandExecutor) {
    BuildResult result = new BuildResult();
    for (String task : build.getTasks()) {
      executeTask(build, config, adjustedProperties, task, result, commandExecutor);
    }

    return result;
  }

  private void executeTask(GradleBuild build, Configuration config, Map<String, String> adjustedProperties, String task,
    final BuildResult result, CommandExecutor commandExecutor) {
    try {
      File projectDir = config.locators().locate(build.getProjectDirectory());

      File gradlew = getGradleWrapper(projectDir);
      checkState(gradlew.exists(), "Gradle wrapper does not exist: '%s'", gradlew.toString());

      Command command = Command.create(gradlew.toString());
      command.setDirectory(projectDir);
      for (Map.Entry<String, String> env : build.getEnvironmentVariables().entrySet()) {
        command.setEnvironmentVariable(env.getKey(), env.getValue());
      }
      command.addArguments(task.split(" "));

      command.addArguments(build.arguments());
      for (Map.Entry<String, String> entry : adjustedProperties.entrySet()) {
        command.addSystemArgument(entry.getKey(), entry.getValue());
      }
      StreamConsumer.Pipe writer = new StreamConsumer.Pipe(result.getLogsWriter());
      LoggerFactory.getLogger(getClass()).info("Execute: {}", command);
      int status = commandExecutor.execute(command, writer, build.getTimeoutSeconds() * 1000);
      result.addStatus(status);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute Gradle", e);
    }
  }

  private File getGradleWrapper(File wrapperDirectory) {
    String gradlewName = "gradlew";
    if (os.isWindows()) {
      gradlewName += ".bat";
    }

    return wrapperDirectory.toPath().resolve(gradlewName).toAbsolutePath().toFile();
  }

  static class Os {
    boolean isWindows() {
      return SystemUtils.IS_OS_WINDOWS;
    }
  }

}
