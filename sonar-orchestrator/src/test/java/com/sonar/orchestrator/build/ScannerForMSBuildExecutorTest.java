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
import com.sonar.orchestrator.util.CommandException;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScannerForMSBuildExecutorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void execute_command() {
    ScannerForMSBuild build = ScannerForMSBuild.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setProjectName("Name")
      .setProjectVersion("1.1")
      .setTimeoutSeconds(30)
      .setDebugLogs(true)
      .setScannerVersion("2.2")
      .setEnvironmentVariable("FOO", "BAR");
    Map<String, String> props = new TreeMap<>();
    props.put("sonar.jdbc.dialect", "h2");

    ScannerForMSBuildInstaller installer = mock(ScannerForMSBuildInstaller.class);
    when(installer.install(eq(Version.create("2.2")), eq(null), any(File.class), eq(false))).thenReturn(new File("SonarScanner.MSBuild.exe"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new ScannerForMSBuildExecutor().execute(build, Configuration.create(), props, installer, executor);

    verify(executor).execute(argThat(c -> c.getDirectory().equals(new File("."))
      && c.toCommandLine().contains("SonarScanner.MSBuild.exe")
      && c.toCommandLine().contains("/d:sonar.verbose=true")
      && c.toCommandLine().contains("/k:SAMPLE")
      && c.toCommandLine().contains("/n:Name")
      && c.toCommandLine().contains("/v:1.1")
      && c.toCommandLine().contains("/d:sonar.jdbc.dialect=h2")
      && c.getEnvironmentVariables().get("FOO").equals("BAR")), any(StreamConsumer.class), eq(30000L));
  }

  @Test
  public void execute_command_with_dot_net_core() {
    ScannerForMSBuild build = ScannerForMSBuild.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setProjectName("Name")
      .setTimeoutSeconds(30)
      .setUseDotNetCore(true)
      .setScannerVersion("4.1.0.1148");

    ScannerForMSBuildInstaller installer = mock(ScannerForMSBuildInstaller.class);
    when(installer.install(eq(Version.create("4.1.0.1148")), eq(null), any(File.class), eq(true))).thenReturn(new File("SonarScanner.MSBuild.dll"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new ScannerForMSBuildExecutor().execute(build, Configuration.create(), new HashMap<>(), installer, executor);

    verify(executor).execute(argThat(c -> {
      String commandLine = c.toCommandLine();
      return c.getDirectory().equals(new File("."))
        && commandLine.startsWith("dotnet")
        && commandLine.contains("SonarScanner.MSBuild.dll")
        && commandLine.contains("/k:SAMPLE")
        && commandLine.contains("/n:Name");
    }), any(StreamConsumer.class), eq(30000L));
  }

  @Test
  public void execute_command_with_dot_net_core_provided() {
    ScannerForMSBuild build = ScannerForMSBuild.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setProjectName("Name")
      .setTimeoutSeconds(30)
      .setDotNetCoreExecutable(new File("/usr/share/dotnet/dotnet"))
      .setScannerVersion("4.1.0.1148");

    ScannerForMSBuildInstaller installer = mock(ScannerForMSBuildInstaller.class);
    when(installer.install(eq(Version.create("4.1.0.1148")), eq(null), any(File.class), eq(true))).thenReturn(new File("SonarScanner.MSBuild.dll"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new ScannerForMSBuildExecutor().execute(build, Configuration.create(), new HashMap<>(), installer, executor);

    verify(executor).execute(argThat(c -> {
      String commandLine = c.toCommandLine();
      return c.getDirectory().equals(new File("."))
        && commandLine.startsWith("/usr/share/dotnet/dotnet")
        && commandLine.contains("SonarScanner.MSBuild.dll")
        && commandLine.contains("/k:SAMPLE")
        && commandLine.contains("/n:Name");
    }), any(StreamConsumer.class), eq(30000L));
  }

  @Test
  public void execute_command_throws_any_exception_should_throw_ISE() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute SonarScanner for .NET");

    ScannerForMSBuild build = ScannerForMSBuild.create()
      .setProjectDir(new File("."))
      .setTimeoutSeconds(30);
    Map<String, String> props = new TreeMap<>();

    ScannerForMSBuildInstaller installer = mock(ScannerForMSBuildInstaller.class);
    when(installer.install(isNull(), isNull(), any(), eq(false))).thenReturn(new File("SonarScanner.MSBuild.exe"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(), anyLong())).thenThrow(new CommandException(mock(Command.class), new Exception("Error")));

    new ScannerForMSBuildExecutor().execute(build, Configuration.create(), props, installer, executor);
  }

  @Test
  public void execute_command_no_param() {
    ScannerForMSBuild build = ScannerForMSBuild.create()
      .setProjectDir(new File("."))
      .setTimeoutSeconds(30);
    Map<String, String> props = new TreeMap<>();

    ScannerForMSBuildInstaller installer = mock(ScannerForMSBuildInstaller.class);
    when(installer.install(isNull(), isNull(), any(), eq(false))).thenReturn(new File("SonarScanner.MSBuild.exe"));
    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(), anyLong())).thenReturn(2);

    new ScannerForMSBuildExecutor().execute(build, Configuration.create(), props, installer, executor);

    verify(executor).execute(argThat(c -> c.getDirectory().equals(new File("."))
      && c.toCommandLine().contains("SonarScanner.MSBuild.exe")), any(), eq(30000L));
  }
}
