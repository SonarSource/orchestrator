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
import com.sonar.orchestrator.build.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SonarScannerExecutorTest {
  @Test
  public void execute_command() {
    SonarScanner build = SonarScanner.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setTimeoutSeconds(30)
      .setDebugLogs(true)
      .setScannerVersion("1.3");
    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");
    props.put("sonar.projectKey", "SAMPLE");

    SonarScannerInstaller installer = mock(SonarScannerInstaller.class);
    when(installer.install(eq(Version.create("1.3")), eq(null), any(File.class))).thenReturn(new File("sonar-runner.sh"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new SonarScannerExecutor().execute(build, Configuration.create(), null, props, installer, executor);

    verify(executor).execute(argThat(c -> c.getDirectory().equals(new File("."))
      && c.toCommandLine().contains("sonar-runner")
      && c.toCommandLine().contains("-X")
      && c.toCommandLine().contains("-Dsonar.jdbc.dialect")
      && c.toCommandLine().contains("-Dsonar.projectKey")), any(), eq(30000L));
  }

  @Test
  public void execute_command_with_additional_args() {
    SonarScanner build = SonarScanner.create()
      .setProjectDir(new File("."))
      .setTimeoutSeconds(30)
      .setScannerVersion("2.0")
      .addArguments("--help");
    Map<String, String> props = new TreeMap<>();

    SonarScannerInstaller installer = mock(SonarScannerInstaller.class);
    when(installer.install(eq(Version.create("2.0")), eq(null), any(File.class))).thenReturn(new File("sonar-scanner.sh"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new SonarScannerExecutor().execute(build, Configuration.create(), null, props, installer, executor);

    verify(executor).execute(argThat(c -> c.getDirectory().equals(new File("."))
      && c.toCommandLine().contains("sonar-scanner.sh")
      && c.toCommandLine().contains("--help")), any(), eq(30000L));
  }

  @Test
  public void execute_native() {
    SonarScanner build = SonarScanner.create().useNative();
    String classifier = build.classifier();
    assertThat(classifier).isNotEmpty();

    Map<String, String> props = new TreeMap<>();

    SonarScannerInstaller installer = mock(SonarScannerInstaller.class);
    when(installer.install(eq(build.scannerVersion()), eq(classifier), any()))
      .thenReturn(new File("dummy.sh"));

    CommandExecutor executor = mock(CommandExecutor.class);

    new SonarScannerExecutor().execute(build, Configuration.create(), null, props, installer, executor);
    verify(installer).install(eq(build.scannerVersion()), eq(classifier), any());
  }
}
