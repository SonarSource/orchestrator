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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.config.Configuration;
import java.io.File;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MockWebServer server = new MockWebServer();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60L));
  private static final MavenLocation SONAR_JAVA_4_5 = MavenLocation.builder()
    .setGroupId("org.sonarsource.java")
    .setArtifactId("sonar-java")
    .setVersion("4.5")
    .build();

  @Test
  public void download_file_from_default_repository() throws Exception {
    prepareDownload("this_is_bytecode");

    Configuration configuration = newConfiguration().build();
    Artifactory underTest = new Artifactory(configuration);

    File targetFile = temp.newFile();
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isTrue();
    assertThat(targetFile).exists().hasContent("this_is_bytecode");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/sonarsource/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request.getHeader("X-JFrog-Art-Api")).isNull();
  }

  @Test
  public void download_file_from_single_defined_repository() throws Exception {
    prepareDownload("this_is_bytecode");

    Configuration configuration = newConfiguration()
      .setProperty("orchestrator.artifactory.repositories", "sonarsource-public-releases")
      .build();
    Artifactory underTest = new Artifactory(configuration);

    File targetFile = temp.newFile();
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isTrue();
    assertThat(targetFile).exists().hasContent("this_is_bytecode");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/sonarsource-public-releases/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request.getHeader("X-JFrog-Art-Api")).isNull();
  }

  @Test
  public void download_file_from_multiple_repositories() throws Exception {
    prepareResponseError(404);
    prepareDownload("this_is_bytecode");

    Configuration configuration = newConfiguration()
      .setProperty("orchestrator.artifactory.repositories", "sonarsource-public-releases,sonarsource-private-releases")
      .build();
    Artifactory underTest = new Artifactory(configuration);

    File targetFile = temp.newFile();
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isTrue();
    assertThat(targetFile).exists().hasContent("this_is_bytecode");

    RecordedRequest request1 = server.takeRequest();
    assertThat(request1.getPath()).isEqualTo("/sonarsource-public-releases/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request1.getHeader("X-JFrog-Art-Api")).isNull();

    RecordedRequest request2 = server.takeRequest();
    assertThat(request2.getPath()).isEqualTo("/sonarsource-private-releases/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request2.getHeader("X-JFrog-Art-Api")).isNull();
  }

  @Test
  public void download_private_file_with_successful_authentication() throws Exception {
    prepareDownload("this_is_bytecode");

    Configuration configuration = newConfiguration()
      .setProperty("orchestrator.artifactory.apiKey", "abcde")
      .build();
    Artifactory underTest = new Artifactory(configuration);

    File targetFile = temp.newFile();
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isTrue();
    assertThat(targetFile).exists().hasContent("this_is_bytecode");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/sonarsource/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request.getHeader("X-JFrog-Art-Api")).isEqualTo("abcde");
  }

  @Test
  public void consider_unauthorized_error_as_artifact_not_found() throws Exception {
    prepareResponseError(401);
    Configuration configuration = newConfiguration().build();

    File targetFile = temp.newFile();
    Artifactory underTest = new Artifactory(configuration);
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isFalse();
  }

  @Test
  public void consider_forbidden_error_as_artifact_not_found() throws Exception {
    prepareResponseError(403);
    Configuration configuration = newConfiguration().build();

    File targetFile = temp.newFile();
    Artifactory underTest = new Artifactory(configuration);
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isFalse();
  }

  @Test
  public void artifact_not_found() throws Exception {
    prepareResponseError(404);
    Configuration configuration = newConfiguration().build();

    File targetFile = temp.newFile();
    Artifactory underTest = new Artifactory(configuration);
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isFalse();
  }

  private Configuration.Builder newConfiguration() throws IOException {
    return Configuration.builder()
      .setProperty("orchestrator.workspaceDir", temp.newFolder().getCanonicalPath())
      .setProperty("orchestrator.artifactory.url", server.url("/").toString());
  }

  private void prepareDownload(String content) {
    server.enqueue(new MockResponse().setBody(content));
  }

  private void prepareResponseError(int status) {
    server.enqueue(new MockResponse().setResponseCode(status));
  }
}
