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
import java.util.Optional;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;

class MavenBuildExecutor extends AbstractBuildExecutor<MavenBuild> {

  private final Os os;

  private static final String MAVEN_OPTS = "MAVEN_OPTS";

  // visible for tests
  MavenBuildExecutor(Os os) {
    this.os = os;
  }

  public MavenBuildExecutor() {
    this(new Os());
  }

  @Override
  BuildResult execute(MavenBuild build, Configuration config, Map<String, String> adjustedProperties, CommandExecutor commandExecutor) {
    BuildResult result = new BuildResult();
    for (String goal : build.getGoals()) {
      appendCoverageArgumentToOpts(build.getEnvironmentVariables(), config, MAVEN_OPTS);
      executeGoal(build, config, adjustedProperties, goal, result, commandExecutor);
    }
    return result;
  }

  private void executeGoal(MavenBuild build, Configuration config, Map<String, String> adjustedProperties, String goal,
    final BuildResult result, CommandExecutor commandExecutor) {
    try {
      File mavenHome = config.fileSystem().mavenHome();
      Command command = Command.create(buildMvnPath(config));
      if (build.getExecutionDir() != null) {
        command.setDirectory(build.getExecutionDir());
      }
      for (Map.Entry<String, String> env : build.getEffectiveEnvironmentVariables().entrySet()) {
        command.setEnvironmentVariable(env.getKey(), env.getValue());
      }
      if (mavenHome != null) {
        // Force M2_HOME to override default value from calling env
        command.setEnvironmentVariable("M2_HOME", mavenHome.getAbsolutePath());
      }
      // allow to set "clean install" in the same process
      command.addArguments(goal.split(" "));
      command.addArgument("-B");
      command.addArgument("-e");

      if (build.getPom() != null) {
        File pomFile = config.locators().locate(build.getPom());
        checkState(pomFile.exists(), "Maven pom does not exist: %s", build.getPom());
        command.addArgument("-f").addArgument(pomFile.getAbsolutePath());
      }
      if (build.isDebugLogs()) {
        command.addArgument("-X");
      }
      command.addArguments(build.arguments());
      for (Map.Entry<String, String> entry : adjustedProperties.entrySet()) {
        command.addSystemArgument(entry.getKey(), entry.getValue());
      }
      StreamConsumer.Pipe writer = new StreamConsumer.Pipe(result.getLogsWriter());
      LoggerFactory.getLogger(getClass()).info("Execute: {}", command);
      int status = commandExecutor.execute(command, writer, build.getTimeoutSeconds() * 1000);
      result.addStatus(status);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute Maven", e);
    }
  }

  String buildMvnPath(Configuration config) throws IOException {
    Optional<String> binary = Optional.ofNullable(config.getStringByKeys("maven.binary", "MAVEN_BINARY"));
    File home = config.fileSystem().mavenHome();
    if (home == null) {
      return binary.orElse(os.isWindows() ? "mvn.cmd" : "mvn");
    }
    if (binary.isPresent()) {
      return new File(home, "bin/" + binary.get()).getCanonicalPath();
    }
    if (os.isWindows()) {
      // .bat is required for maven versions <= 3.2
      File bin = new File(home, "bin/mvn.bat");
      if (bin.exists()) {
        return bin.getCanonicalPath();
      }
      return new File(home, "bin/mvn.cmd").getCanonicalPath();
    }
    return new File(home, "bin/mvn").getCanonicalPath();
  }

  static class Os {
    boolean isWindows() {
      return SystemUtils.IS_OS_WINDOWS;
    }
  }
}
