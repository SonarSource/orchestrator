/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import com.sonar.orchestrator.db.Database;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.argThat;
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
    Database database = mock(Database.class);
    when(database.getSonarProperties()).thenReturn(ImmutableMap.of("sonar.jdbc.dialect", "h2", "sonar.jdbc.url", "jdbc:h2"));
    Server server = mock(Server.class);
    when(server.getUrl()).thenReturn("http://localhost:9000");
    Build build = mock(Build.class);
    when(build.getProperties()).thenReturn(ImmutableMap.of("sonar.projectKey", "SAMPLE", "language", "java"));

    BuildRunner runner = new BuildRunner(config, database);
    runner.runQuietly(server, build);

    verify(build).execute(eq(config), argThat(new BaseMatcher<Map<String, String>>() {
      @Override
      public boolean matches(Object o) {
        Map<String, String> props = (Map<String, String>) o;
        return props.get("sonar.projectKey").equals("SAMPLE")
          && props.get("sonar.jdbc.dialect").equals("h2")
          && props.get("sonar.jdbc.url").equals("jdbc:h2")
          && props.get("sonar.host.url").equals("http://localhost:9000")
          && props.get("language").equals("java");
      }

      @Override
      public void describeTo(Description description) {
      }
    }));
  }

  @Test
  public void runQuietly() {
    Configuration config = Configuration.create();
    Database database = mock(Database.class);
    Server server = mock(Server.class);
    Build build = mock(Build.class);
    when(build.execute(any(Configuration.class), anyMap())).thenReturn(new BuildResult().setStatus(2));

    BuildRunner runner = new BuildRunner(config, database);
    BuildResult result = runner.runQuietly(server, build);

    assertThat(result.getStatus()).isEqualTo(2);
  }

  @Test
  public void throw_exception_if_run_fails() {
    thrown.expect(BuildFailureException.class);
    thrown.expectMessage("status=2");

    Build build = mock(Build.class);
    try {
      Configuration config = Configuration.create();
      Database database = mock(Database.class);
      Server server = mock(Server.class);
      when(build.execute(any(Configuration.class), anyMap())).thenReturn(new BuildResult().setStatus(2));

      BuildRunner runner = new BuildRunner(config, database);
      runner.run(server, build);

    } catch (BuildFailureException e) {
      assertThat(e.getBuild()).isEqualTo(build);
      assertThat(e.getResult().isSuccess()).isFalse();
      assertThat(e.getResult().isSuccess()).isFalse();
      assertThat(e.getMessage()).startsWith("status=2");
      // forward exception to the junit test
      throw e;
    }
  }

  @Test
  public void return_status_zero_if_run_passes() {
    Configuration config = Configuration.create();
    Database database = mock(Database.class);
    Server server = mock(Server.class);
    Build build = mock(Build.class);
    when(build.execute(any(Configuration.class), anyMap())).thenReturn(new BuildResult().setStatus(0));

    BuildRunner runner = new BuildRunner(config, database);
    BuildResult result = runner.run(server, build);

    assertThat(result.isSuccess()).isTrue();
  }
}
