/*
 * Orchestrator
 * Copyright (C) 2011-2019 SonarSource SA
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
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;

class AntBuildExecutor extends AbstractBuildExecutor<AntBuild> {

  @Override
  BuildResult execute(AntBuild build, Configuration config, Map<String, String> adjustedProperties, CommandExecutor commandExecutor) {
    BuildResult result = new BuildResult();
    for (String target : build.getTargets()) {
      executeTarget(build, config, adjustedProperties, target, result, commandExecutor);
    }
    return result;
  }

  private void executeTarget(AntBuild build, Configuration config, Map<String, String> adjustedProperties, String target,
    BuildResult result, CommandExecutor commandExecutor) {
    try {
      File home = build.getAntHome() != null ? build.getAntHome() : config.fileSystem().antHome();
      Command command = Command.create(getAntPath(home));
      for (Map.Entry<String, String> env : build.getEnvironmentVariables().entrySet()) {
        command.setEnvironmentVariable(env.getKey(), env.getValue());
      }
      command.addArguments(target.split(" "));

      File antFile = config.locators().locate(build.getBuildLocation());
      checkState(antFile.exists(), "Ant build file does not exist: %s", build.getBuildLocation());

      command.addArgument("-f").addArgument(antFile.getCanonicalPath());
      command.addArguments(build.arguments());
      for (Map.Entry entry : adjustedProperties.entrySet()) {
        command.addSystemArgument(entry.getKey().toString(), entry.getValue().toString());
      }
      LoggerFactory.getLogger(getClass()).info("Execute: {}", command);
      StreamConsumer.Pipe writer = new StreamConsumer.Pipe(result.getLogsWriter());
      int status = commandExecutor.execute(command, writer, build.getTimeoutSeconds() * 1000);
      result.addStatus(status);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute Ant", e);
    }
  }

  private static String getAntPath(@Nullable File antHome) throws IOException {
    String program = "ant";
    if (SystemUtils.IS_OS_WINDOWS) {
      program += ".bat";
    }
    if (antHome != null) {
      program = new File(antHome, "bin/" + program).getCanonicalPath();
    }
    return program;
  }

}
