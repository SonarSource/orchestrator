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
import com.sonar.orchestrator.util.Command;
import com.sonar.orchestrator.util.CommandExecutor;
import com.sonar.orchestrator.util.StreamConsumer;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PropertyFilterRunner.class)
public class AntBuildExecutorTest {
  @Test
  public void execute_command() {
    final FileLocation buildFile = FileLocation.of("src/test/resources/com/sonar/orchestrator/build/AntBuildTest/build.xml");
    AntBuild build = AntBuild.create()
            .setBuildLocation(buildFile)
            .setTimeoutSeconds(30)
            .setTargets("sonar");

    Map<String, String> props = Maps.newTreeMap();
    props.put("sonar.jdbc.dialect", "h2");

    CommandExecutor executor = mock(CommandExecutor.class);
    when(executor.execute(any(Command.class), any(StreamConsumer.class), anyLong())).thenReturn(2);

    new AntBuildExecutor().execute(build, Configuration.create(), props, executor);

    verify(executor).execute(argThat(new BaseMatcher<Command>() {
      @Override
      public boolean matches(Object o) {
        Command c = (Command) o;
        return c.toCommandLine().contains("ant")
          && c.toCommandLine().contains("-f")
          && c.toCommandLine().contains("-Dsonar.jdbc.dialect=");
      }

      @Override
      public void describeTo(Description description) {
      }
    }), any(StreamConsumer.class), eq(30000L));
  }
}
