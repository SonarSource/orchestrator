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
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import java.io.File;
import java.util.Map;
import org.slf4j.LoggerFactory;

class SonarScannerExecutor extends AbstractBuildExecutor<SonarScanner> {

  private static final String SONAR_SCANNER_OPTS = "SONAR_SCANNER_OPTS";

  @Override
  BuildResult execute(SonarScanner build, Configuration config, Map<String, String> adjustedProperties, CommandExecutor create) {
    return execute(build, config, adjustedProperties, new SonarScannerInstaller(config.locators()), create);
  }

  BuildResult execute(SonarScanner build, Configuration config, Map<String, String> adjustedProperties, SonarScannerInstaller installer,
    CommandExecutor commandExecutor) {
    BuildResult result = new BuildResult();
    File scannerScript = installer.install(build.scannerVersion(), build.classifier(), config.fileSystem().workspace());
    try {
      appendCoverageArgumentToOpts(build.getEnvironmentVariables(), config, SONAR_SCANNER_OPTS);
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

}
