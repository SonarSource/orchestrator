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
package com.sonar.orchestrator.build;

import com.google.common.collect.Maps;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.junit.PropertyFilterRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import org.apache.commons.lang.SystemUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(PropertyFilterRunner.class)
public class MavenBuildExecutorTest {

  @Test
  public void testDebugMode() throws IOException {
    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(MavenBuildExecutor.getMvnPath(new File("maven"))).contains("bin\\mvn.bat");
    } else {
      assertThat(MavenBuildExecutor.getMvnPath(new File("maven"))).contains("bin/mvn");
    }
  }

  @Test
  public void shouldStoreLogs() {
    Location pom = FileLocation.of(getClass().getResource("/com/sonar/orchestrator/build/MavenBuildTest/pom.xml"));
    MavenBuild build = MavenBuild.create(pom).addGoal("clean");

    BuildResult result = new MavenBuildExecutor().execute(build, Configuration.createEnv(), Maps.<String, String>newHashMap());

    assertThat(result.getLogs().length()).isGreaterThan(0);
    assertThat(result.getLogs()).contains("[INFO] BUILD SUCCESS");
  }

  // ORCH-179
  @Test
  public void shouldPassAdditionalArguments() {
    Location pom = FileLocation.of(getClass().getResource("/com/sonar/orchestrator/build/MavenBuildTest/pom.xml"));
    MavenBuild build = MavenBuild.create(pom).addGoal("clean").addArguments("-PnotExists");

    BuildResult result = new MavenBuildExecutor().execute(build, Configuration.createEnv(), Maps.<String, String>newHashMap());

    assertThat(result.getLogs().length()).isGreaterThan(0);
    assertThat(result.getLogs()).contains("[WARNING] The requested profile \"notExists\" could not be activated because it does not exist.");
  }

  @Test
  public void execute_command() throws Exception {
    File pom = new File(getClass().getResource("/com/sonar/orchestrator/build/MavenBuildTest/pom.xml").toURI());
    MavenBuild build = MavenBuild.create(pom)
      .addGoal("clean")
      .addSonarGoal()
      .setDebugLogs(true)
      .setTimeoutSeconds(30);
    Map<String, String> props = Maps.newTreeMap();
    props.put("sonar.jdbc.dialect", "h2");

    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new MavenBuildExecutor().execute(build, Configuration.create(), props, executor);

    verify(executor).execute(argThat(mvnMatcher(pom, "clean")), any(StreamConsumer.class), eq(30000L));
    verify(executor).execute(argThat(mvnMatcher(pom, "sonar:sonar")), any(StreamConsumer.class), eq(30000L));
  }

  private BaseMatcher<Command> mvnMatcher(final File pom, final String goal) {
    return new BaseMatcher<Command>() {
      @Override
      public boolean matches(Object o) {
        Command c = (Command) o;
        return c.toCommandLine().contains("mvn")
          && c.toCommandLine().contains("-f " + pom.getAbsolutePath())
          && c.toCommandLine().contains("-X")
          && c.toCommandLine().contains(goal);
      }

      @Override
      public void describeTo(Description description) {
      }
    };
  }
}
