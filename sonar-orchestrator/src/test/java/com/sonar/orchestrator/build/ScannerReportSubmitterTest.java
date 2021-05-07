/*
 * Orchestrator
 * Copyright (C) 2011-2021 SonarSource SA
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
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ScannerReportSubmitterTest {
  private final int randomPort = 1 + new Random().nextInt(49152);
  private ScannerReportSubmitter scannerReportSubmitter;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MockWebServer server = new MockWebServer();
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void before() {
    scannerReportSubmitter = new ScannerReportSubmitter(newServer());
  }

  @Test
  public void submit_report() throws IOException, InterruptedException {
    File scannerDir = temp.newFolder();
    FileUtils.write(new File(scannerDir, "test"), "content", UTF_8);
    BuildCache.CachedReport cachedReport = new BuildCache.CachedReport(scannerDir.toPath(), "projectKey", "projectName", null, null);

    server.enqueue(new MockResponse());
    scannerReportSubmitter.submit(cachedReport);
    RecordedRequest receivedRequest = server.takeRequest();
    assertThat(receivedRequest.getMethod()).isEqualTo("POST");
    assertThat(receivedRequest.getPath()).isEqualTo("/api/ce/submit?projectKey=projectKey&projectName=projectName");
    String body = receivedRequest.getBody().readUtf8();
    assertThat(body).contains("Content-Type: application/zip");
    assertThat(body).contains("Content-Disposition: form-data; name=\"report\"; filename=\"report.zip\"");
  }

  @Test
  public void submit_report_with_branch() throws IOException, InterruptedException {
    File scannerDir = temp.newFolder();
    FileUtils.write(new File(scannerDir, "test"), "content", UTF_8);
    BuildCache.CachedReport cachedReport = new BuildCache.CachedReport(scannerDir.toPath(), "projectKey", "projectName", "branch", null);

    server.enqueue(new MockResponse());
    scannerReportSubmitter.submit(cachedReport);
    RecordedRequest receivedRequest = server.takeRequest();
    assertThat(receivedRequest.getMethod()).isEqualTo("POST");
    assertThat(receivedRequest.getPath()).isEqualTo("/api/ce/submit?projectKey=projectKey&projectName=projectName&branch=branch&branchType=BRANCH");
    String body = receivedRequest.getBody().readUtf8();
    assertThat(body).contains("Content-Type: application/zip");
    assertThat(body).contains("Content-Disposition: form-data; name=\"report\"; filename=\"report.zip\"");
  }

  @Test
  public void submit_report_with_pr() throws IOException, InterruptedException {
    File scannerDir = temp.newFolder();
    FileUtils.write(new File(scannerDir, "test"), "content", UTF_8);
    BuildCache.CachedReport cachedReport = new BuildCache.CachedReport(scannerDir.toPath(), "projectKey", "projectName", null, "pr1");

    server.enqueue(new MockResponse());
    scannerReportSubmitter.submit(cachedReport);
    RecordedRequest receivedRequest = server.takeRequest();
    assertThat(receivedRequest.getMethod()).isEqualTo("POST");
    assertThat(receivedRequest.getPath()).isEqualTo("/api/ce/submit?projectKey=projectKey&projectName=projectName&pullRequest=pr1");
    String body = receivedRequest.getBody().readUtf8();
    assertThat(body).contains("Content-Type: application/zip");
    assertThat(body).contains("Content-Disposition: form-data; name=\"report\"; filename=\"report.zip\"");
  }

  @Test
  public void fail_if_pr_and_branch_are_set() {
    BuildCache.CachedReport cachedReport = new BuildCache.CachedReport(Paths.get("fake"), "projectKey", "projectName", "branch", "pr1");
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Cannot be pull request and branch at the same time");
    scannerReportSubmitter.submit(cachedReport);
  }

  private Server newServer() {
    String url = server.url("").toString();
    return new Server(mock(Locators.class), mock(File.class), Edition.COMMUNITY, Version.create("9.0"), HttpUrl.parse(url), randomPort, null);
  }
}
