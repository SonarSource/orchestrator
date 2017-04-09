/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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

import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.version.Version;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SynchronousAnalyzerTest {

  @Rule
  public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

  @Test
  public void wait_as_long_queue_is_not_empty() {
    Server server = mock(Server.class);
    HttpCall httpCall = mock(HttpCall.class, Mockito.RETURNS_DEEP_STUBS);
    when(httpCall.execute().getBodyAsString()).thenReturn("false", "false", "true");
    when(server.newHttpCall(SynchronousAnalyzer.RELATIVE_PATH)).thenReturn(httpCall);
    when(server.version()).thenReturn(Version.create("5.0"));

    new SynchronousAnalyzer(server, 1L, 2).waitForDone();

    // fast enough to finish before junit timeout
    verify(server, times(3)).newHttpCall(SynchronousAnalyzer.RELATIVE_PATH);
  }

  @Test
  public void default_settings() {
    Server server = mock(Server.class);
    SynchronousAnalyzer analyzer = new SynchronousAnalyzer(server);
    assertThat(analyzer.getDelayMs()).isEqualTo(100L);
    assertThat(analyzer.getLogFrequency()).isEqualTo(10);
  }
}
