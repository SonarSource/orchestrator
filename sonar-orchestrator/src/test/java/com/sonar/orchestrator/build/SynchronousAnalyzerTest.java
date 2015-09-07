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

import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.junit.PropertyFilterRunner;
import com.sonar.orchestrator.version.Version;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PropertyFilterRunner.class)
public class SynchronousAnalyzerTest {

  @Rule
  public Timeout timeout = new Timeout(3000, TimeUnit.MILLISECONDS);

  @Test
  public void do_nothing_before_5_0() {
    Server server = mock(Server.class);
    when(server.version()).thenReturn(Version.create("4.5"));

    new SynchronousAnalyzer(server).waitForDone();

    // fast enough to finish before junit timeout
    verify(server, never()).post(anyString(), anyMap());
  }

  @Test
  public void wait_as_long_queue_is_not_empty() {
    Server server = mock(Server.class);
    when(server.version()).thenReturn(Version.create("5.0"));
    when(server.post(SynchronousAnalyzer.RELATIVE_URL, Collections.<String, Object>emptyMap()))
      .thenReturn("false", "false", "true");

    new SynchronousAnalyzer(server, 10L, 2).waitForDone();

    // fast enough to finish before junit timeout
    verify(server, times(3)).post(SynchronousAnalyzer.RELATIVE_URL, Collections.<String, Object>emptyMap());
  }

  @Test
  public void default_settings() {
    Server server = mock(Server.class);
    SynchronousAnalyzer analyzer = new SynchronousAnalyzer(server);
    assertThat(analyzer.getDelayMs()).isEqualTo(100L);
    assertThat(analyzer.getLogFrequency()).isEqualTo(10);
  }
}
