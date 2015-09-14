/*
 * Orchestrator
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ServerWrapperTest {

  DefaultExecutor executor = mock(DefaultExecutor.class, Mockito.RETURNS_DEEP_STUBS);
  ServerWatcher watcher = mock(ServerWatcher.class);
  Server server;
  SonarDistribution distribution = new SonarDistribution();
  File homeDir;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void prepare() throws IOException {
    homeDir = temp.newFolder();
    new File(homeDir, "lib").mkdir();
    FileUtils.getFile(homeDir, "extensions", "jdbc-driver").mkdirs();
    server = new Server(mock(FileSystem.class), homeDir, distribution);
    server.setPort(1234);
    server.setUrl("http://localhost:1234");
  }

  @Test
  public void start_and_stop_version_4_5() throws IOException {
    distribution.setVersion(Version.create("4.5"));

    FileUtils.touch(new File(homeDir, "lib/sonar-application-4.5.jar"));

    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), homeDir, executor, watcher);
    assertThat(wrapper.isStarted()).isFalse();
    wrapper.start();
    assertThat(wrapper.isStarted()).isTrue();

    wrapper.stop();

    // waiting for server to receive shutdown request
    assertThat(wrapper.isStarted()).isFalse();
  }

  @Test
  public void fail_to_start() throws IOException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("initial error");

    distribution.setVersion(Version.create("4.5"));
    doThrow(new IllegalStateException("initial error")).when(watcher).execute(eq(executor), any(CommandLine.class));

    FileUtils.touch(new File(homeDir, "lib/sonar-application-4.5.jar"));

    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), homeDir, executor, watcher);
    wrapper.start();
  }

  @Test
  public void fail_if_missing_4_5_libs() {
    distribution.setVersion(Version.create("4.5"));

    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), homeDir, executor, watcher);
    try {
      wrapper.start();
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("No or too many sonar-application-*.jar files found in: " + new File(homeDir, "lib").getAbsolutePath());
      assertThat(wrapper.isStarted()).isFalse();
      wrapper.stop();
    }
  }

  @Test
  public void do_not_fail_on_server_shutdown_error() throws IOException, InterruptedException {
    distribution.setVersion(Version.create("4.5"));
    FileUtils.touch(new File(homeDir, "lib/sonar-application-4.5.jar"));

    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), temp.newFolder(), executor, watcher);
    wrapper.start();

    doThrow(new IllegalStateException()).when(watcher).waitFor(anyInt());
    wrapper.stop();

    // no error
    assertThat(wrapper.isStarted()).isFalse();
  }

  @Test
  public void do_not_fail_on_shutdown_if_server_is_down() throws IOException {
    distribution.setVersion(Version.create("4.5"));
    FileUtils.touch(new File(homeDir, "lib/sonar-application-4.5.jar"));

    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), temp.newFolder(), executor, watcher);
    assertThat(wrapper.isStarted()).isFalse();

    wrapper.stop();

    assertThat(wrapper.isStarted()).isFalse();
  }

  @Test
  public void do_not_start_server_twice() throws IOException {
    distribution.setVersion(Version.create("4.5"));
    FileUtils.touch(new File(homeDir, "lib/sonar-application-4.5.jar"));

    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), temp.newFolder(), executor, watcher);
    wrapper.start();

    try {
      wrapper.start();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Server is already started");
    }
  }

  @Test
  public void test_default_constructor() throws IOException {
    FileUtils.touch(new File(homeDir, "lib/sonar-application-4.5.jar"));
    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), temp.newFolder());
    assertThat(wrapper.executor()).isNotNull();
    assertThat(wrapper.executor().getWatchdog()).isNotNull();
  }

  @Test
  public void fail_if_version_before_45() throws IOException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Minimum supported version of SonarQube is 4.5. Got 4.2");
    distribution.setVersion(Version.create("4.2"));
    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), temp.newFolder(), executor, watcher);
    wrapper.start();
  }

  @Test
  public void build_command_line_with_unknown_javaHome() throws IOException {
    distribution.setVersion(Version.create("4.5"));
    FileUtils.touch(new File(homeDir, "lib/sonar-application-4.5.jar"));

    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), null, executor, watcher);
    CommandLine commandLine = wrapper.buildCommandLine();

    assertThat(commandLine.getExecutable()).isEqualTo("java");
  }

  @Test
  public void build_command_line_with_valid_javaHome_and_additional_jvm_arguments() throws IOException {
    distribution.setVersion(Version.create("4.5"));
    distribution.serverAdditionalJvmArguments().add("-additionalArgument");
    FileUtils.touch(new File(homeDir, "lib/sonar-application-4.5.jar"));

    ServerWrapper wrapper = new ServerWrapper(server, Configuration.create(), homeDir, executor, watcher);
    CommandLine commandLine = wrapper.buildCommandLine();

    assertThat(commandLine.getExecutable()).isEqualTo(homeDir.getAbsolutePath() + "/bin/java");
    assertThat(commandLine.getArguments()).contains("-additionalArgument");
  }
}
