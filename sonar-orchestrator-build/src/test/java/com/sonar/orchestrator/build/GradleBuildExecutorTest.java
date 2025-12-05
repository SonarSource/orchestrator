/*
 * Orchestrator Build
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
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.build.command.Command;
import com.sonar.orchestrator.build.command.CommandExecutor;
import com.sonar.orchestrator.build.util.StreamConsumer;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GradleBuildExecutorTest {

  @Test
  public void execute_unix() {
    FileLocation projectDir = FileLocation.of(getClass().getResource("/com/sonar/orchestrator/build/GradleBuildTest"));
    GradleBuild build = GradleBuild.create(projectDir)
      .setTasks("clean", "sonarqube")
      .setTimeoutSeconds(30)
      .setEnvironmentVariable("GRADLE_OPTS", "-Xmx512m");

    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");

    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    GradleBuildExecutor.Os os = mock(GradleBuildExecutor.Os.class);
    when(os.isWindows()).thenReturn(false);

    Configuration config = Configuration.create();
    new GradleBuildExecutor(os).execute(build, config, new Locators(config), props, executor);

    verify(executor).execute(argThat(gradlewMatcher(projectDir.getFile(), "gradlew", "clean")), any(), eq(30000L));
    verify(executor).execute(argThat(gradlewMatcher(projectDir.getFile(), "gradlew", "sonarqube")), any(), eq(30000L));
  }

  @Test
  public void execute_windows() {
    FileLocation projectDir = FileLocation.of(getClass().getResource("/com/sonar/orchestrator/build/GradleBuildTest"));
    GradleBuild build = GradleBuild.create(projectDir)
      .setTasks("clean", "sonarqube")
      .setTimeoutSeconds(30)
      .setEnvironmentVariable("GRADLE_OPTS", "-Xmx512m");

    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");

    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    GradleBuildExecutor.Os os = mock(GradleBuildExecutor.Os.class);
    when(os.isWindows()).thenReturn(true);

    Configuration config = Configuration.create();
    new GradleBuildExecutor(os).execute(build, config, new Locators(config), props, executor);

    verify(executor).execute(argThat(gradlewMatcher(projectDir.getFile(), "gradlew.bat", "clean")), any(), eq(30000L));
    verify(executor).execute(argThat(gradlewMatcher(projectDir.getFile(), "gradlew.bat", "sonarqube")), any(), eq(30000L));
  }

  private ArgumentMatcher<Command> gradlewMatcher(File workingDirectory, String gradlewName, String task) {
    return c ->
      // Working directory
      c.getDirectory().equals(workingDirectory) &&
      // Environment variables
      c.getEnvironmentVariables().get("GRADLE_OPTS").equals("-Xmx512m") &&
      // The actual command line contents
      c.toCommandLine().contains(gradlewName) &&
      c.toCommandLine().contains(task) &&
      c.toCommandLine().contains("-Dsonar.jdbc.dialect=");
  }

  @Test(expected = IllegalStateException.class)
  public void execute_invalid_path() {
    Location projectDir = FileLocation.of("no_such_path");
    GradleBuild build = GradleBuild.create(projectDir)
      .setTasks("clean", "sonarqube");

    CommandExecutor executor = mock(CommandExecutor.class);
    Configuration config = Configuration.create();
    new GradleBuildExecutor().execute(build, config, new Locators(config), new HashMap<>(), executor);
  }
}
