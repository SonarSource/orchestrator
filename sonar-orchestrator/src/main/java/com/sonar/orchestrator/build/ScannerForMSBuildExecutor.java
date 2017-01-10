/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import java.io.File;
import java.util.Map;
import org.slf4j.LoggerFactory;

class ScannerForMSBuildExecutor extends AbstractBuildExecutor<ScannerForMSBuild> {

  @Override
  BuildResult execute(ScannerForMSBuild build, Configuration config, Map<String, String> adjustedProperties, CommandExecutor create) {
    return execute(build, config, adjustedProperties, new ScannerForMSBuildInstaller(config), create);
  }

  @VisibleForTesting
  BuildResult execute(ScannerForMSBuild build, Configuration config, Map<String, String> adjustedProperties, ScannerForMSBuildInstaller installer,
    CommandExecutor commandExecutor) {
    BuildResult result = new BuildResult();
    File runnerScript = installer.install(build.scannerVersion(), build.getLocation(), config.fileSystem().workspace(), build.isUseOldRunnerScript());
    try {
      Command command = createCommand(build, adjustedProperties, runnerScript);
      LoggerFactory.getLogger(ScannerForMSBuild.class).info("Execute: {}", command);
      StreamConsumer.Pipe writer = new StreamConsumer.Pipe(result.getLogsWriter());
      int status = commandExecutor.execute(command, writer, build.getTimeoutSeconds() * 1000);
      result.addStatus(status);
      return result;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute SonarQube Scanner", e);
    }
  }

  private static Command createCommand(ScannerForMSBuild build, Map<String, String> adjustedProperties, File runnerScript) {
    Command command = Command.create(runnerScript.getAbsolutePath());
    command.setDirectory(build.getProjectDir());
    for (Map.Entry<String, String> env : build.getEffectiveEnvironmentVariables().entrySet()) {
      command.setEnvironmentVariable(env.getKey(), env.getValue());
    }
    if (build.isDebugLogs()) {
      command.addArgument("/d:sonar.verbose=true");
    }
    if (build.getProjectKey() != null) {
      command.addArgument("/k:" + build.getProjectKey());
    }
    if (build.getProjectName() != null) {
      command.addArgument("/n:" + build.getProjectName());
    }
    if (build.getProjectVersion() != null) {
      command.addArgument("/v:" + build.getProjectVersion());
    }

    command.addArguments(build.arguments());

    for (Map.Entry<String, String> entry : adjustedProperties.entrySet()) {
      if (entry.getValue() != null) {
        addProp(command, entry.getKey(), entry.getValue());
      }
    }
    return command;
  }

  public static void addProp(Command c, String key, String value) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(value);

    StringBuilder sb = new StringBuilder();
    sb.append("/d:").append(key).append("=").append(value);
    c.addArgument(sb.toString());
  }

}
