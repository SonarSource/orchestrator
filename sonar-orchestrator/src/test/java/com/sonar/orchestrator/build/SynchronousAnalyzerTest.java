/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.version.Version;
import java.util.concurrent.TimeUnit;
import okhttp3.Credentials;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SynchronousAnalyzerTest {

  @Rule
  public Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
  @Rule
  public MockWebServer webServer = new MockWebServer();

  @Test
  public void wait_as_long_queue_is_not_empty() throws Exception {
    webServer.enqueue(new MockResponse().setBody("false"));
    webServer.enqueue(new MockResponse().setBody("false"));
    webServer.enqueue(new MockResponse().setBody("true"));

    Server server = new Server(null, null, Edition.COMMUNITY, Version.create("7.3.0.1000"), webServer.url(""), 9001, null);
    new SynchronousAnalyzer(server, 1L, 2).waitForDone();

    // fast enough to finish before junit timeout
    assertThat(webServer.getRequestCount()).isEqualTo(3);
    for (int i = 0; i < 3; i++) {
      RecordedRequest recordedRequest = webServer.takeRequest();
      assertThat(recordedRequest.getPath()).isEqualTo(SynchronousAnalyzer.RELATIVE_PATH);
      assertThat(recordedRequest.getHeader("Authorization")).isEqualTo(Credentials.basic("admin", "admin"));
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
