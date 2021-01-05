/*
 * Orchestrator
 * Copyright (C) 2011-2021 SonarSource SA
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
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarScannerExecutorTest {
  @Test
  public void execute_command() {
    SonarRunner build = SonarRunner.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setTimeoutSeconds(30)
      .setDebugLogs(true)
      .setRunnerVersion("1.3");
    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");
    props.put("sonar.projectKey", "SAMPLE");

    SonarScannerInstaller installer = mock(SonarScannerInstaller.class);
    when(installer.install(eq(Version.create("1.3")), eq(null), any(File.class), eq(true))).thenReturn(new File("sonar-runner.sh"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new SonarScannerExecutor().execute(build, Configuration.create(), props, installer, executor);

    verify(executor).execute(argThat(c -> c.getDirectory().equals(new File("."))
      && c.toCommandLine().contains("sonar-runner")
      && c.toCommandLine().contains("-X")
      && c.toCommandLine().contains("-Dsonar.jdbc.dialect")
      && c.toCommandLine().contains("-Dsonar.projectKey")), any(), eq(30000L));
  }

  @Test
  public void execute_task() {
    SonarRunner build = SonarRunner.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setTimeoutSeconds(30)
      .setRunnerVersion("2.1")
      .setTask("my-task");
    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");
    props.put("sonar.projectKey", "SAMPLE");

    SonarScannerInstaller installer = mock(SonarScannerInstaller.class);
    when(installer.install(eq(Version.create("2.1")), eq(null), any(File.class), eq(true))).thenReturn(new File("sonar-runner.sh"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new SonarScannerExecutor().execute(build, Configuration.create(), props, installer, executor);

    verify(executor).execute(argThat(c -> {
      // Windows directory with space use case
      String quote = "";
      if (c.getDirectory().getAbsolutePath().contains(" ")) {
        quote = "\"";
      }
      return c.getDirectory().equals(new File("."))
        && c.toCommandLine().contains("sonar-runner.sh" + quote + " my-task")
        && c.toCommandLine().contains("-e")
        && c.toCommandLine().contains("-Dsonar.jdbc.dialect")
        && c.toCommandLine().contains("-Dsonar.projectKey");
    }), any(), eq(30000L));
  }

  @Test
  public void execute_command_with_task_old_syntax() {
    SonarRunner build = SonarRunner.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setTimeoutSeconds(30)
      .setRunnerVersion("2.0")
      .setTask("my-task");
    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");
    props.put("sonar.projectKey", "SAMPLE");

    SonarScannerInstaller installer = mock(SonarScannerInstaller.class);
    when(installer.install(eq(Version.create("2.0")), eq(null), any(File.class), eq(true))).thenReturn(new File("sonar-runner.sh"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new SonarScannerExecutor().execute(build, Configuration.create(), props, installer, executor);

    verify(executor).execute(argThat(c -> c.getDirectory().equals(new File("."))
      && c.toCommandLine().contains("sonar-runner")
      && c.toCommandLine().contains("-Dsonar.jdbc.dialect")
      && c.toCommandLine().contains("-Dsonar.projectKey")
      && c.toCommandLine().contains("-Dsonar.task")), any(), eq(30000L));
  }

  @Test
  public void execute_command_with_additional_args() {
    SonarRunner build = SonarRunner.create()
      .setProjectDir(new File("."))
      .setTimeoutSeconds(30)
      .setRunnerVersion("2.0")
      .addArguments("--help");
    Map<String, String> props = new TreeMap<>();

    SonarScannerInstaller installer = mock(SonarScannerInstaller.class);
    when(installer.install(eq(Version.create("2.0")), eq(null), any(File.class), eq(true))).thenReturn(new File("sonar-runner.sh"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new SonarScannerExecutor().execute(build, Configuration.create(), props, installer, executor);

    verify(executor).execute(argThat(c -> c.getDirectory().equals(new File("."))
      && c.toCommandLine().contains("sonar-runner.sh")
      && c.toCommandLine().contains("--help")), any(), eq(30000L));
  }

  @Test
  public void execute_native() {
    SonarScanner build = SonarScanner.create().useNative();
    String classifier = build.classifier();
    assertThat(classifier).isNotEmpty();

    Map<String, String> props = new TreeMap<>();

    SonarScannerInstaller installer = mock(SonarScannerInstaller.class);
    when(installer.install(eq(build.scannerVersion()), eq(classifier), any(), eq(false)))
      .thenReturn(new File("dummy.sh"));

    CommandExecutor executor = mock(CommandExecutor.class);

    new SonarScannerExecutor().execute(build, Configuration.create(), props, installer, executor);
    verify(installer).install(eq(build.scannerVersion()), eq(classifier), any(), eq(false));
  }
}
