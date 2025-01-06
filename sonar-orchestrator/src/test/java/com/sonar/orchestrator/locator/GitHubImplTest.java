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
package com.sonar.orchestrator.locator;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GitHubImplTest {
  @Rule
  public MockWebServer server = new MockWebServer();

  @Test
  public void getLatestScannerReleaseVersion_returns_valid_version() {
    prepareResponse("1.2.3.4");

    GitHubImpl sut = new GitHubImpl(server.url("/").toString());
    String latestVersion = sut.getLatestScannerReleaseVersion();
    assertThat(latestVersion).isEqualTo("1.2.3.4");
  }

  @Test
  public void getLatestScannerReleaseVersion_two_calls_make_a_single_request() {
    prepareResponse("version");

    GitHubImpl sut = new GitHubImpl(server.url("/").toString());
    String first = sut.getLatestScannerReleaseVersion();
    assertThat(first).isEqualTo("version");
    String second = sut.getLatestScannerReleaseVersion();
    assertThat(second).isEqualTo("version");
    assertThat(server.getRequestCount()).isEqualTo(1);
  }

  @After
  public void resetCache() {
    GitHubImpl.resetCache();
  }

  private void prepareResponse(String version) {
    server.enqueue(new MockResponse().setBody("{ \"tag_name\": \"" + version + "\" }"));
  }
}
