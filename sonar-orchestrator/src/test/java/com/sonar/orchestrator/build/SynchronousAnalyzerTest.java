/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource Sàrl
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

import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.version.Version;
import java.util.concurrent.TimeUnit;
import mockwebserver3.junit4.MockWebServerRule;
import okhttp3.Credentials;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SynchronousAnalyzerTest {

  @Rule
  public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
  @Rule
  public MockWebServerRule mockWebServerRule = new MockWebServerRule();

  @Test
  public void wait_as_long_queue_is_not_empty() throws Exception {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body("false").build());
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body("false").build());
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body("true").build());

    Server server = new Server(null, null, Edition.COMMUNITY, Version.create("7.3.0.1000"), mockWebServerRule.getServer().url(""), 9001, null);
    new SynchronousAnalyzer(server, 1L, 2).waitForDone();

    // fast enough to finish before junit timeout
    assertThat(mockWebServerRule.getServer().getRequestCount()).isEqualTo(3);
    for (int i = 0; i < 3; i++) {
      RecordedRequest recordedRequest = mockWebServerRule.getServer().takeRequest();
      assertThat(recordedRequest.getTarget()).isEqualTo(SynchronousAnalyzer.RELATIVE_PATH);
      assertThat(recordedRequest.getHeaders().get("Authorization")).isEqualTo(Credentials.basic("admin", "admin"));
    }
  }

  @Test
  public void test_default_settings() {
    Server server = mock(Server.class);
    SynchronousAnalyzer analyzer = new SynchronousAnalyzer(server);
    assertThat(analyzer.getDelayMs()).isEqualTo(100L);
    assertThat(analyzer.getLogFrequency()).isEqualTo(10);
  }
}
