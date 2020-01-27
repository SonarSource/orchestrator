/*
 * Orchestrator
 * Copyright (C) 2011-2020 SonarSource SA
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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Random;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MockWebServer server = new MockWebServer();

  int randomPort = 1 + new Random().nextInt(49152);

  @Test
  public void getUrl_does_not_return_trailing_slash() {
    Server underTest = newServerForUrl("http://localhost:9999/sonarqube");
    assertThat(underTest.getUrl()).isEqualTo("http://localhost:9999/sonarqube");

    underTest = newServerForUrl("http://localhost:9999/sonarqube/");
    assertThat(underTest.getUrl()).isEqualTo("http://localhost:9999/sonarqube");

    underTest = newServerForUrl("http://localhost:9999");
    assertThat(underTest.getUrl()).isEqualTo("http://localhost:9999");
  }

  @Test
  public void getSearchPort_returns_constructor_parameter() {
    Server server = newServerForUrl(randomPort);

    assertThat(server.getSearchPort()).isEqualTo(randomPort);
  }

  @Test
  public void getClusterNodeName_returns_constructor_parameter() {
    Server standaloneServer = newServerForClusterNodeName(null);
    assertThat(standaloneServer.getClusterNodeName()).isEmpty();

    String nodeName = "fooBar";
    Server clusterServer = newServerForClusterNodeName(nodeName);
    assertThat(clusterServer.getClusterNodeName()).contains(nodeName);
  }

  @Test
  public void restoreProfile_sends_POST_request() throws Exception {
    File backup = temp.newFile();
    FileUtils.write(backup, "<backup/>");
    server.enqueue(new MockResponse());
    File home = temp.newFolder();
    FileUtils.touch(new File(home, "lib/sonar-application-6.3.0.1234.jar"));
    Locators locators = mock(Locators.class);
    when(locators.openInputStream(any())).thenReturn(FileUtils.openInputStream(backup));
    Server underTest = new Server(locators, home, Edition.COMMUNITY, Version.create("6.3.0.1234"),
      HttpUrl.parse(this.server.url("").toString()), randomPort, null);

    underTest.restoreProfile(FileLocation.of(backup));

    RecordedRequest receivedRequest = server.takeRequest();
    assertThat(receivedRequest.getMethod()).isEqualTo("POST");
    assertThat(receivedRequest.getPath()).isEqualTo("/api/qualityprofiles/restore");
    // sent as multipart form
    assertThat(receivedRequest.getBody().readUtf8())
      .contains("Content-Disposition: form-data; name=\"backup\"")
      .contains("Content-Length: 9")
      .contains("<backup/>");
  }

  @Test
  public void provisionProject_sends_POST_request() throws Exception {
    server.enqueue(new MockResponse());
    Server underTest = newServerForUrl(this.server.url("").toString());

    underTest.provisionProject("foo", "Foo");

    RecordedRequest receivedRequest = server.takeRequest();
    assertThat(receivedRequest.getMethod()).isEqualTo("POST");
    assertThat(receivedRequest.getPath()).isEqualTo("/api/projects/create");
    assertThat(receivedRequest.getBody().readUtf8()).isEqualTo("project=foo&name=Foo");
  }

  @Test
  public void associateProjectToQualityProfile_sends_POST_request() throws Exception {
    server.enqueue(new MockResponse());
    Server underTest = newServerForUrl(this.server.url("").toString());

    underTest.associateProjectToQualityProfile("foo", "bar", "baz");

    RecordedRequest receivedRequest = server.takeRequest();
    assertThat(receivedRequest.getMethod()).isEqualTo("POST");
    assertThat(receivedRequest.getPath()).isEqualTo("/api/qualityprofiles/add_project");
    assertThat(receivedRequest.getBody().readUtf8()).isEqualTo("project=foo&language=bar&qualityProfile=baz");
  }

  private Server newServerForUrl(String url) {
    return newServerForUrl(url, randomPort, null);
  }

  private Server newServerForUrl(int searchPort) {
    return newServerForUrl("", searchPort, null);
  }

  private Server newServerForClusterNodeName(@Nullable String clusterNodeName) {
    return newServerForUrl("", randomPort, clusterNodeName);
  }

  private Server newServerForUrl(String url, int searchPort, @Nullable String clusterNodeName) {
    return new Server(mock(Locators.class), mock(File.class), Edition.COMMUNITY, Version.create("7.3.0.1000"), HttpUrl.parse(url), searchPort, clusterNodeName);
  }

}
