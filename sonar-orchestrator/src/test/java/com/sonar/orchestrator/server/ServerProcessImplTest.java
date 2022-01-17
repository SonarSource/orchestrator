/*
 * Orchestrator
 * Copyright (C) 2011-2022 SonarSource SA
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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.TestModules;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.exec.CommandLine;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ServerProcessImplTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TestRule safeguard = new DisableOnDebug(new Timeout(30, TimeUnit.SECONDS));

  private ServerCommandLineFactory commandLineFactory = mock(ServerCommandLineFactory.class);
  private Server server = mock(Server.class);
  private StartupLogWatcher logWatcher = mock(StartupLogWatcher.class);
  private ServerProcessImpl underTest = new ServerProcessImpl(commandLineFactory, server, logWatcher);

  @Test
  public void successfully_start_and_stop() throws Exception {
    prepareValidCommand("com.sonar.orchestrator.echo.SonarQubeEmulator");
    underTest.start();

    verify(logWatcher).isStarted("starting");
    verify(logWatcher).isStarted("started");

    underTest.stop();
    verify(logWatcher).isStarted("stopped");
  }

  @Test
  public void can_not_start_twice() throws Exception {
    prepareValidCommand("com.sonar.orchestrator.echo.SonarQubeEmulator");
    underTest.start();
    try {
      expectedException.expectMessage("Server is already started");
      expectedException.expect(IllegalStateException.class);
      underTest.start();
    } finally {
      underTest.stop();
    }
  }

  @Test
  public void can_stop_if_not_started() throws Exception {
    prepareValidCommand("com.sonar.orchestrator.echo.SonarQubeEmulator");
    underTest.start();
    underTest.stop();

    // following stops do not fail
    underTest.stop();
    underTest.stop();
  }

  @Test
  public void fail_if_command_can_not_be_executed() throws Exception {
    when(server.version()).thenReturn(Version.create("6.7"));

    // execute an invalid command from an invalid directory. That should fail.
    File invalidDir = temp.newFolder();
    when(server.getHome()).thenReturn(invalidDir);
    invalidDir.delete();
    when(commandLineFactory.create(server)).thenReturn(new CommandLine("command_does_not_exist"));

    expectedException.expectMessage("Can not execute command");
    underTest.start();
  }

  @Test
  public void fail_if_server_fails_to_start() throws Exception {
    prepareValidCommand("com.sonar.orchestrator.echo.Fail");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Server startup failure");
    underTest.start();

    verify(logWatcher).isStarted("starting");
    verify(logWatcher).isStarted("error");

    underTest.stop();
  }

  @Test
  public void fail_if_server_version_is_older_than_6_2() throws Exception {
    when(server.version()).thenReturn(Version.create("5.6"));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Minimum supported version of SonarQube is 6.2. Got 5.6.");
    underTest.start();
  }

  @Test
  public void fail_on_timeout_if_process_is_alive_but_not_correctly_started() throws IOException {
    prepareValidCommand("com.sonar.orchestrator.echo.Stuck");
    underTest.setStartupTimeout(1);
    // process is stuck, force kill. Let's reduce the regular stop command to
    // 1ms so that test does not wait for 5 minutes before force the kill
    underTest.setStopTimeout(1);

    expectedException.expectMessage("Server did not start in timely fashion");
    underTest.start();

    assertThat(underTest.isProcessAlive()).isFalse();
  }

  private void prepareValidCommand(String mainClass) throws IOException {
    when(server.version()).thenReturn(Version.create("6.7"));
    when(server.getHome()).thenReturn(new File("../echo/target"));
    File jar = TestModules.getFile("../echo/target", "echo-*.jar");
    when(logWatcher.isStarted("started")).thenReturn(true);

    when(commandLineFactory.create(server)).thenReturn(
      new CommandLine("java")
      .addArgument("-cp")
      .addArgument(jar.getCanonicalPath())
      .addArgument(mainClass)
    );
  }


}
