/*
 * Orchestrator
 * Copyright (C) 2011-2018 SonarSource SA
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StartupLogListenerTest {

  private StartupLogWatcher watcher = mock(StartupLogWatcher.class);

  @Test
  public void isStarted_returns_true_as_soon_as_startup_log_is_displayed() {
    when(watcher.isStarted("Process[web] is up")).thenReturn(true);
    StartupLogListener underTest = new StartupLogListener(watcher);

    assertThat(underTest.isStarted()).isFalse();

    underTest.processLine("foo", 2);
    assertThat(underTest.isStarted()).isFalse();

    underTest.processLine("Process[web] is up", 2);
    assertThat(underTest.isStarted()).isTrue();

    // still started, do not change flag status
    underTest.processLine("foo", 2);
    assertThat(underTest.isStarted()).isTrue();
  }

}
