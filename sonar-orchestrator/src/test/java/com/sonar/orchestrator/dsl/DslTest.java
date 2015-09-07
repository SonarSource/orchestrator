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

import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.junit.PropertyFilterRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Map;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(PropertyFilterRunner.class)
public class DslTest {
  @Test
  public void should_compile_and_execute_commands() {
    Command command1 = mock(Command.class);
    Command command2 = mock(Command.class);
    SonarItDslInterpreter dsl = mock(SonarItDslInterpreter.class);
    when(dsl.interpret("start server")).thenReturn(Arrays.asList(command1, command2));

    Dsl.Context context = new Dsl.Context();
    new Dsl(dsl).execute("start server", context);

    verify(dsl).interpret("start server");
    verify(command1).execute(context);
    verify(command2).execute(context);
    verifyNoMoreInteractions(command1, command2);
  }

  @Test
  public void should_stop_orchestrator_on_error() {
    Dsl.Context context = mock(Dsl.Context.class);
    Command command1 = mock(Command.class);
    doThrow(new IllegalArgumentException()).when(command1).execute(context);
    SonarItDslInterpreter dsl = mock(SonarItDslInterpreter.class);
    when(dsl.interpret("start server")).thenReturn(Arrays.asList(command1));

    try {
      new Dsl(dsl).execute("start server", context);
      fail("Should throw an exception");
    } catch (IllegalStateException e) {
      verify(context).stop();
    }
  }

  @Test
  public void should_stop_orchestrator() {
    Orchestrator orchestrator = mock(Orchestrator.class);
    Dsl.Context context = new Dsl.Context();
    context.setOrchestrator(orchestrator);
    assertThat(context.getOrchestrator()).isSameAs(orchestrator);
    context.stop();
    verify(orchestrator).stop();
  }

  @Test
  public void should_not_fail_if_orchestrator_is_not_initialized() {
    Dsl.Context context = new Dsl.Context();
    assertThat(context.getOrchestrator()).isNull();
    context.stop();
    // no error
  }

  @Test
  public void should_keep_settings() {
    Dsl.Context context = new Dsl.Context();
    assertThat(context.getSettings()).isEmpty();

    Map<String, String> settings = Maps.newHashMap();
    context.setSettings(settings);
    assertThat(context.getSettings()).isEqualTo(settings);
  }
}
