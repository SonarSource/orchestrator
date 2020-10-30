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
package com.sonar.orchestrator.locator;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.sonar.orchestrator.config.Configuration;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryImplTest {

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
  public void download_file_with_success() throws Exception {
    prepareDownload("this_is_bytecode");

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);

    File targetFile = temp.newFile();
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isTrue();
    assertThat(targetFile).exists().hasContent("this_is_bytecode");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/sonarsource/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request.getHeader("X-JFrog-Art-Api")).isNull();
  }

  @Test
  public void download_file_from_second_repository() throws Exception {
    prepareResponseError(403);
    prepareDownload("this_is_bytecode");

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);

    File targetFile = temp.newFile();
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isTrue();
    assertThat(targetFile).exists().hasContent("this_is_bytecode");

    RecordedRequest request1 = server.takeRequest();
    assertThat(request1.getPath()).isEqualTo("/sonarsource/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request1.getHeader("X-JFrog-Art-Api")).isNull();

    RecordedRequest request2 = server.takeRequest();
    assertThat(request2.getPath()).isEqualTo("/sonarsource-qa/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request2.getHeader("X-JFrog-Art-Api")).isNull();
  }

  @Test
  public void download_private_file_with_successful_authentication() throws Exception {
    prepareDownload("this_is_bytecode");

    Configuration configuration = newConfiguration()
      .setProperty("orchestrator.artifactory.apiKey", "abcde")
      .build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);

    File targetFile = temp.newFile();
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isTrue();
    assertThat(targetFile).exists().hasContent("this_is_bytecode");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/sonarsource/org/sonarsource/java/sonar-java/4.5/sonar-java-4.5.jar");
    assertThat(request.getHeader("X-JFrog-Art-Api")).isEqualTo("abcde");
  }

  @Test
  public void download_considers_unauthorized_error_as_artifact_not_found() throws Exception {
    prepareResponseError(401);
    prepareResponseError(401);
    prepareResponseError(401);
    Configuration configuration = newConfiguration().build();

    File targetFile = temp.newFile();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isFalse();
  }

  @Test
  public void download_considers_forbidden_error_as_artifact_not_found() throws Exception {
    prepareResponseError(403, "not found");
    prepareResponseError(403, "not found");
    prepareResponseError(403, "not found");
    Configuration configuration = newConfiguration().build();

    File targetFile = temp.newFile();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isFalse();
  }

  @Test
  public void download_artifact_not_found() throws Exception {
    prepareResponseError(404);
    prepareResponseError(404);
    prepareResponseError(404);
    Configuration configuration = newConfiguration().build();

    File targetFile = temp.newFile();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    boolean found = underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);

    assertThat(found).isFalse();
  }

  @Test
  public void download_throws_ISE_if_unexpected_error() throws Exception {
    prepareResponseError(500);
    Configuration configuration = newConfiguration().build();
    File targetFile = temp.newFile();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(new TypeSafeMatcher<String>() {
      @Override
      public void describeTo(Description description) {

      }

      @Override
      protected boolean matchesSafely(String s) {
        return s.startsWith("Failed to request");
      }
    });

    underTest.downloadToFile(SONAR_JAVA_4_5, targetFile);
  }

  @Test
  public void resolveVersion_returns_version_if_not_an_alias() throws Exception {
    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);

    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "7.1.0.1234");
    Optional<String> version = underTest.resolveVersion(location);

    assertThat(version).hasValue(location.getVersion());
  }

  @Test
  public void resolveVersion_throws_ISE_if_deprecated_LTS_alias() throws Exception {
    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "LTS");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unsupported version alias for [org.sonarsource.sonarqube:sonar-plugin-api:LTS:jar]");

    underTest.resolveVersion(location);
  }

  @Test
  public void resolveVersion_throws_ISE_if_deprecated_OLDEST_COMPATIBLE_alias() throws Exception {
    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "OLDEST_COMPATIBLE");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Unsupported version alias for [org.sonarsource.sonarqube:sonar-plugin-api:OLDEST_COMPATIBLE:jar]");

    underTest.resolveVersion(location);
  }

  @Test
  public void resolveVersion_resolves_LATEST_RELEASE_alias() throws Exception {
    prepareVersions("6.7", "7.0", "7.1", "7.1.1", "6.7.3");

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "LATEST_RELEASE");

    Optional<String> version = underTest.resolveVersion(location);
    assertThat(version).hasValue("7.1.1");

    verifyVersionsRequest(location.getGroupId(), location.getArtifactId(), "*", "sonarsource-releases");
  }

  @Test
  public void resolveVersion_resolves_LATEST_RELEASE_alias_as_empty_if_no_versions() throws Exception {
    prepareVersions();

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "LATEST_RELEASE");

    Optional<String> version = underTest.resolveVersion(location);
    assertThat(version).isEmpty();
  }

  @Test
  public void resolveVersion_resolves_LATEST_RELEASE_alias_of_series() throws Exception {
    prepareVersions("7.1.0.1000", "7.1.1.1500", "7.1.0.1400");

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "LATEST_RELEASE[7.1]");

    Optional<String> version = underTest.resolveVersion(location);
    assertThat(version).hasValue("7.1.1.1500");

    verifyVersionsRequest(location.getGroupId(), location.getArtifactId(), "7.1*", "sonarsource-releases");
  }

  @Test
  public void resolveVersion_resolves_LATEST_RELEASE_alias_of_major_series() throws Exception {
    prepareVersions("7.1.0.1000", "7.2.1.1500", "7.1.0.1400");

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "LATEST_RELEASE[7]");

    Optional<String> version = underTest.resolveVersion(location);
    assertThat(version).hasValue("7.2.1.1500");

    verifyVersionsRequest(location.getGroupId(), location.getArtifactId(), "7*", "sonarsource-releases");
  }

  @Test
  public void resolveVersion_resolves_DEV_alias() throws Exception {
    prepareVersions("6.7.0.1000", "7.0.0.1010", "7.1.0.1030", "7.1.0.1020");

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "DEV");

    Optional<String> version = underTest.resolveVersion(location);
    assertThat(version).hasValue("7.1.0.1030");

    verifyVersionsRequest(location.getGroupId(), location.getArtifactId(), "*", "sonarsource-builds");
  }

  @Test
  public void resolveVersion_resolves_DEV_alias_of_series() throws Exception {
    prepareVersions("7.1.0.1030", "7.1.0.1020");

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "DEV[7.1]");

    Optional<String> version = underTest.resolveVersion(location);
    assertThat(version).hasValue("7.1.0.1030");

    verifyVersionsRequest(location.getGroupId(), location.getArtifactId(), "7.1*", "sonarsource-builds");
  }

  @Test
  public void resolveVersion_resolves_DOGFOOD_alias() throws Exception {
    prepareVersions("6.7.0.1000", "7.0.0.1010", "7.1.0.1030", "7.1.0.1020");

    Configuration configuration = newConfiguration().build();
    ArtifactoryImpl underTest = ArtifactoryImpl.create(configuration);
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "DOGFOOD");

    Optional<String> version = underTest.resolveVersion(location);
    assertThat(version).hasValue("7.1.0.1030");

    verifyVersionsRequest(location.getGroupId(), location.getArtifactId(), "*", "sonarsource-dogfood-builds");
  }

  @Test
  public void resolveVersion_sends_access_token_if_defined() throws Exception {
    prepareVersions("7.1", "7.2");
    Configuration configuration = newConfiguration()
      .setProperty("orchestrator.artifactory.apiKey", "abcde")
      .build();

    ArtifactoryImpl.create(configuration).resolveVersion(MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "LATEST_RELEASE"));

    RecordedRequest request = server.takeRequest();
    assertThat(request.getHeader("X-JFrog-Art-Api")).isEqualTo("abcde");
  }

  @Test
  public void resolveVersion_throws_ISE_on_response_error() throws Exception {
    prepareResponseError(502);
    Configuration configuration = newConfiguration()
      .build();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(new TypeSafeMatcher<String>() {
      @Override
      public void describeTo(Description description) {
      }

      @Override
      protected boolean matchesSafely(String s) {
        return s.startsWith("Fail to request versions at");
      }
    });

    ArtifactoryImpl.create(configuration).resolveVersion(MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "LATEST_RELEASE"));
  }

  private void verifyVersionsRequest(String groupId, String artifactId, String versionLayout, String repositories) throws InterruptedException {
    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/api/search/versions?g=" + groupId + "&a=" + artifactId + "&remote=0&repos=" + repositories + "&v=" + versionLayout);
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
    prepareResponseError(status, null);
  }

  private void prepareResponseError(int status, @Nullable String message) {
    MockResponse response = new MockResponse()
            .setResponseCode(status);
    if(message != null) {
      response.setBody(new JsonObject().add(
              "errors", new JsonArray().add(
                      new JsonObject().add("message", message)))
              .toString());
    }
    server.enqueue(response);
  }

  private void prepareVersions(String... versions) {
    JsonArray array = Json.array();
    for (String version : versions) {
      array.add(Json.object().add("version", version).add("integration", false));
    }
    JsonValue json = Json.object()
      .add("results", array);
    server.enqueue(new MockResponse().setBody(json.toString()));
  }
}
