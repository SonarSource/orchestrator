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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.container.Server;
import java.io.File;
import java.io.IOException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerCommandLineFactoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private FileSystem fs = mock(FileSystem.class);
  private Server server = mock(Server.class);

  @Test
  public void create_command_line() throws Exception {
    generateValidFileSystem();

    ServerCommandLineFactory underTest = new ServerCommandLineFactory(fs);

    CommandLine commandLine = underTest.create(server);
    assertThat(commandLine.getExecutable()).isEqualTo("java");
    assertThat(commandLine.getArguments())
      .contains("-Xmx32m")
      .contains("-Djava.net.preferIPv4Stack=true")
      .contains("lib/sonar-application-5.6.jar");
  }

  @Test
  public void override_java_home() throws Exception {
    generateValidFileSystem();
    File javaHome = temp.newFolder();
    when(fs.javaHome()).thenReturn(javaHome);

    ServerCommandLineFactory underTest = new ServerCommandLineFactory(fs);

    CommandLine commandLine = underTest.create(server);
    assertThat(new File(commandLine.getExecutable())).isEqualTo(new File(javaHome, "bin/java"));
  }

  @Test
  public void fail_if_libs_are_missing() throws Exception {
    generateValidFileSystem();
    FileUtils.deleteQuietly(new File(server.getHome(), "lib/sonar-application-5.6.jar"));

    ServerCommandLineFactory underTest = new ServerCommandLineFactory(fs);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No or too many sonar-application-*.jar files found in: ");
    underTest.create(server);
  }

  private void generateValidFileSystem() throws IOException {
    File homeDir = temp.getRoot();
    FileUtils.touch(new File(homeDir, "lib/sonar-application-5.6.jar"));
    when(server.getHome()).thenReturn(homeDir);
  }
}
