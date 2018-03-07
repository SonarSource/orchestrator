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
package com.sonar.orchestrator.container;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ServerTest {

  private static final HttpUrl A_URL = HttpUrl.parse("http://localhost:9000");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MockWebServer server = new MockWebServer();

  @Test
  public void getUrl_does_not_return_trailing_slash() throws Exception {
    Server underTest = newServerForUrl("http://localhost:9999/sonarqube");
    assertThat(underTest.getUrl()).isEqualTo("http://localhost:9999/sonarqube");

    underTest = newServerForUrl("http://localhost:9999/sonarqube/");
    assertThat(underTest.getUrl()).isEqualTo("http://localhost:9999/sonarqube");

    underTest = newServerForUrl("http://localhost:9999");
    assertThat(underTest.getUrl()).isEqualTo("http://localhost:9999");
  }

  @Test
  public void guess_version_from_installed_files() throws Exception {
    File home = temp.newFolder();
    FileUtils.touch(new File(home, "lib/sonar-application-6.3.0.1234.jar"));

    Server underTest = new Server(mock(FileSystem.class), home, new SonarDistribution(), A_URL);

    assertThat(underTest.version()).isEqualTo(Version.create("6.3.0.1234"));
  }

  @Test
  public void version_throws_ISE_if_installation_dir_does_not_exist() throws Exception {
    File home = temp.newFolder();
    deleteDirectory(home);
    Server underTest = new Server(mock(FileSystem.class), home, new SonarDistribution(), A_URL);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Installation incomplete, missing directory " + home.getAbsolutePath());

    underTest.version();
  }

  @Test
  public void version_throws_ISE_if_app_jar_is_missing() throws Exception {
    // libs dir exists, but does not contain the expected jar
    File home = temp.newFolder();
    forceMkdir(new File(home, "lib"));
    Server underTest = new Server(mock(FileSystem.class), home, new SonarDistribution(), A_URL);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No files match");

    underTest.version();
  }

  @Test
  public void restoreProfile_sends_POST_request() throws Exception {
    File backup = temp.newFile();
    FileUtils.write(backup, "<backup/>");
    server.enqueue(new MockResponse());
    File home = temp.newFolder();
    FileUtils.touch(new File(home, "lib/sonar-application-6.3.0.1234.jar"));
    Server underTest = new Server(new FileSystem(temp.newFolder(), Configuration.builder().build()), home, new SonarDistribution(),
      HttpUrl.parse(this.server.url("").toString()));

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
    assertThat(receivedRequest.getBody().readUtf8()).isEqualTo("key=foo&name=Foo");
  }

  private Server newServerForUrl(String url) throws IOException {
    FileSystem fs = new FileSystem(temp.newFolder(), Configuration.builder().build());
    return new Server(fs, mock(File.class), new SonarDistribution(), HttpUrl.parse(url));
  }

}
