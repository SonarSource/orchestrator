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
package com.sonar.orchestrator.dsl;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.junit.PropertyFilterRunner;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(PropertyFilterRunner.class)
public class SonarRunnerCommandTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_execute_sonar_runner() throws IOException {
    Orchestrator orchestrator = mock(Orchestrator.class);
    Dsl.Context context = new Dsl.Context().setOrchestrator(orchestrator);
    final File dir = temp.newFolder();
    context.setState(CdCommand.PWD_KEY, dir);
    SonarRunnerCommand command = spy(new SonarRunnerCommand());
    doNothing().when(command).executeBuild(eq(context), any(SonarRunner.class));
    command.setProperty("sonar.java.version", "1.5");

    command.execute(context);

    verify(command).executeBuild(eq(context), argThat(new BaseMatcher<SonarRunner>() {
      @Override
      public boolean matches(Object o) {
        SonarRunner runner = (SonarRunner) o;
        return dir.equals(runner.getProjectDir()) && "1.5".equals(runner.getProperty("sonar.java.version"));
      }

      @Override
      public void describeTo(Description description) {
      }
    }));
  }

  @Test
  public void should_fail_if_dir_is_not_set() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The command 'cd' has not been executed");

    Orchestrator orchestrator = mock(Orchestrator.class);
    Dsl.Context context = new Dsl.Context().setOrchestrator(orchestrator);
    SonarRunnerCommand command = new SonarRunnerCommand();
    command.execute(context);
  }

  @Test
  public void should_fail_if_orchestrator_is_not_initialized() throws IOException {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("The command 'start server' has not been executed");

    Dsl.Context context = new Dsl.Context().setState(CdCommand.PWD_KEY, temp.newFolder());
    SonarRunnerCommand command = new SonarRunnerCommand();
    command.execute(context);
  }
}
