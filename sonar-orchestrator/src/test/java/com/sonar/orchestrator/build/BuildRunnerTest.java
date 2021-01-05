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

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.version.Version;
import java.util.Arrays;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BuildRunnerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void inject_server_properties() {
    Configuration config = Configuration.create();
    Server server = mock(Server.class);
    when(server.version()).thenReturn(Version.create("5.2"));
    when(server.getUrl()).thenReturn("http://localhost:9000");
    Build build = mock(Build.class);
    when(build.getProperties()).thenReturn(ImmutableMap.of("sonar.projectKey", "SAMPLE", "language", "java"));

    BuildRunner runner = new BuildRunner(config);
    runner.runQuietly(server, build);

    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);

    verify(build).execute(eq(config), captor.capture());

    assertThat(captor.getValue()).containsOnly(
      entry("sonar.projectKey", "SAMPLE"),
      entry("sonar.host.url", "http://localhost:9000"),
      entry("language", "java"),
      entry("sonar.scm.disabled", "true"),
      entry("sonar.branch.autoconfig.disabled", "true"));
  }

  @Test
  public void inject_properties_for_msbuild_start() {
    Configuration config = Configuration.create();
    Server server = mock(Server.class);
    when(server.version()).thenReturn(Version.create("5.2"));
    when(server.getUrl()).thenReturn("http://localhost:9000");
    Build build = mock(ScannerForMSBuild.class);
    when(build.getProperties()).thenReturn(ImmutableMap.of("sonar.projectKey", "SAMPLE", "language", "java"));

    BuildRunner runner = new BuildRunner(config);
    runner.runQuietly(server, build);

    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);

    verify(build).execute(eq(config), captor.capture());

    assertThat(captor.getValue()).containsOnly(
      entry("sonar.projectKey", "SAMPLE"),
      entry("sonar.host.url", "http://localhost:9000"),
      entry("language", "java"),
      entry("sonar.scm.disabled", "true"),
      entry("sonar.branch.autoconfig.disabled", "true"));
  }

  @Test
  public void dont_inject_properties_for_msbuild_end() {
    Configuration config = Configuration.create();
    Server server = mock(Server.class);
    when(server.version()).thenReturn(Version.create("5.2"));
    when(server.getUrl()).thenReturn("http://localhost:9000");
    Build build = mock(ScannerForMSBuild.class);
    when(build.arguments()).thenReturn(Arrays.asList("end"));

    BuildRunner runner = new BuildRunner(config);
    runner.runQuietly(server, build);

    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);

    verify(build).execute(eq(config), captor.capture());

    assertThat(captor.getValue()).isEmpty();

  }

  @Test
  public void runQuietly() {
    Configuration config = Configuration.create();
    Server server = mock(Server.class);
    when(server.version()).thenReturn(Version.create("5.6"));
    Build build = mock(Build.class);
    when(build.execute(any(Configuration.class), anyMap())).thenReturn(new BuildResult().addStatus(2));

    BuildRunner runner = new BuildRunner(config);
    BuildResult result = runner.runQuietly(server, build);

    assertThat(result.getLastStatus()).isEqualTo(2);
    assertThat(result.getStatuses()).containsExactly(2);
  }

  @Test
  public void throw_exception_if_run_fails() {
    thrown.expect(BuildFailureException.class);
    thrown.expectMessage("statuses=[2]");

    Build build = mock(Build.class);
    try {
      Configuration config = Configuration.create();
      Server server = mock(Server.class);
      when(server.version()).thenReturn(Version.create("5.6"));
      when(build.execute(any(Configuration.class), anyMap())).thenReturn(new BuildResult().addStatus(2));

      BuildRunner runner = new BuildRunner(config);
      runner.run(server, build);

    } catch (BuildFailureException e) {
      assertThat(e.getBuild()).isEqualTo(build);
      assertThat(e.getResult().isSuccess()).isFalse();
      assertThat(e.getResult().isSuccess()).isFalse();
      assertThat(e.getMessage()).startsWith("statuses=[2]");
      // forward exception to the junit test
      throw e;
    }
  }

  @Test
  public void throw_exception_if_any_run_fails() {
    thrown.expect(BuildFailureException.class);
    thrown.expectMessage("statuses=[2, 0]");

    Build build = mock(Build.class);
    try {
      Configuration config = Configuration.create();
      Server server = mock(Server.class);
      when(server.version()).thenReturn(Version.create("5.6"));
      when(build.execute(any(Configuration.class), anyMap())).thenReturn(new BuildResult().addStatus(2).addStatus(0));

      BuildRunner runner = new BuildRunner(config);
      runner.run(server, build);

    } catch (BuildFailureException e) {
      assertThat(e.getBuild()).isEqualTo(build);
      assertThat(e.getResult().isSuccess()).isFalse();
      assertThat(e.getResult().isSuccess()).isFalse();
      assertThat(e.getMessage()).startsWith("statuses=[2, 0]");
      // forward exception to the junit test
      throw e;
    }
  }

  @Test
  public void return_status_zero_if_run_passes() {
    Configuration config = Configuration.create();
    Server server = mock(Server.class);
    when(server.version()).thenReturn(Version.create("5.6"));
    Build build = mock(Build.class);
    when(build.execute(any(Configuration.class), anyMap())).thenReturn(new BuildResult().addStatus(0));

    BuildRunner runner = new BuildRunner(config);
    BuildResult result = runner.run(server, build);

    assertThat(result.isSuccess()).isTrue();
  }
}
