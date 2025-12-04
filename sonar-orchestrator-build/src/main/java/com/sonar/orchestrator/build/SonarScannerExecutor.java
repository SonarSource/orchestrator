/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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
import com.sonar.orchestrator.build.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import java.io.File;
import java.util.Map;
import org.slf4j.LoggerFactory;

class SonarScannerExecutor extends AbstractBuildExecutor<SonarScanner> {

  private static final String SONAR_SCANNER_OPTS = "SONAR_SCANNER_OPTS";

  private static Command createCommand(SonarScanner build, Map<String, String> adjustedProperties, File runnerScript) {
    Command command = Command.create(runnerScript.getAbsolutePath());
    command.setDirectory(build.getProjectDir());
    for (Map.Entry<String, String> env : build.getEffectiveEnvironmentVariables().entrySet()) {
      command.setEnvironmentVariable(env.getKey(), env.getValue());
    }
    if (build.isDebugLogs()) {
      command.addArgument("-X");
    } else if (build.isShowErrors()) {
      command.addArgument("-e");
    }

    command.addArguments(build.arguments());
    for (Map.Entry<String, String> entry : adjustedProperties.entrySet()) {
      if (entry.getValue() != null) {
        command.addSystemArgument(entry.getKey(), entry.getValue());
      }
    }
    return command;
  }

  @Override
  BuildResult execute(SonarScanner build, Configuration config, Locators locators, Map<String, String> adjustedProperties, CommandExecutor create) {
    return execute(build, config, locators, adjustedProperties, new SonarScannerInstaller(locators), create);
  }

  BuildResult execute(SonarScanner build, Configuration config, Locators locators, Map<String, String> adjustedProperties, SonarScannerInstaller installer,
    CommandExecutor commandExecutor) {
    BuildResult result = new BuildResult();
    File scannerScript = installer.install(build.scannerVersion(), build.classifier(), config.fileSystem().workspace().toFile());
    try {
      appendCoverageArgumentToOpts(build.getEnvironmentVariables(), config, locators, SONAR_SCANNER_OPTS);
      Command command = createCommand(build, adjustedProperties, scannerScript);
      LoggerFactory.getLogger(SonarScanner.class).info("Execute: {}", command);
      StreamConsumer.Pipe writer = new StreamConsumer.Pipe(result.getLogsWriter());
      int status = commandExecutor.execute(command, writer, build.getTimeoutSeconds() * 1000);
      result.addStatus(status);
      return result;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute SonarQube Scanner", e);
    }
  }

}
