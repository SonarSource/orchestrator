/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;

public class GradleBuildExecutor extends AbstractBuildExecutor<GradleBuild> {

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
      File gradlew = config.locators().locate(getGradleWrapper(build.getProjectDirectory().toString()));
      checkState(gradlew.exists(), "Gradle wrapper does not exist: '%s'", gradlew.toString());

      Command command = Command.create(gradlew.toString());
      for (Map.Entry<String, String> env : build.getEnvironmentVariables().entrySet()) {
        command.setEnvironmentVariable(env.getKey(), env.getValue());
      }
      command.addArguments(task.split(" "));

      command.addArguments(build.arguments());
      for (Map.Entry entry : adjustedProperties.entrySet()) {
        command.addSystemArgument(entry.getKey().toString(), entry.getValue().toString());
      }
      StreamConsumer.Pipe writer = new StreamConsumer.Pipe(result.getLogsWriter());
      LoggerFactory.getLogger(getClass()).info("Execute: {}", command);
      int status = commandExecutor.execute(command, writer, build.getTimeoutSeconds() * 1000);
      result.addStatus(status);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute Gradle", e);
    }
  }

  private static Location getGradleWrapper(String wrapperDirectoryLocation) {
    return FileLocation.of(Paths.get(wrapperDirectoryLocation, gradleWrapperName()).toAbsolutePath().toFile());
  }

  private static String gradleWrapperName() {
    if (SystemUtils.IS_OS_WINDOWS) {
      return "gradlew.bat";
    }
    return "gradlew";
  }
}
