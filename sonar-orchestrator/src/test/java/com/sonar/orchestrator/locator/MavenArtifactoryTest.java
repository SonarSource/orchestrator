/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit4.MockWebServerRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class MavenArtifactoryTest {

  private static final MavenLocation SONAR_PLUGIN_API = MavenLocation.builder()
    .setGroupId("org.sonarsource.sonarqube")
    .setArtifactId("sonar-plugin-api")
    .setVersion("3.0")
    .build();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public MockWebServerRule mockWebServerRule = new MockWebServerRule();
  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60L));

  private static String getContent() {
    return "<metadata>" +
      "<groupId>org.sonarsource.sonarqube</groupId>" +
      "<artifactId>sonar-plugin-api</artifactId>" +
      "<versioning>" +
      "<latest>3.0.1.54424</latest>" +
      "<release>3.0.1.54424</release>" +
      "<versions>" +
      "<version>1.0</version>" +
      "<version>1.0.1</version>" +
      "<version>1.1</version>" +
      "<version>2.0</version>" +
      "<version>2.0.1</version>" +
      "<version>3.0</version>" +
      "<version>3.0.1</version>" +
      "<version>3.0.1.54424</version>" +
      "</versions>" +
      "<lastUpdated>20221018072817</lastUpdated>" +
      "</versioning>" +
      "</metadata>";
  }

  @Test
  public void downloadToFile_whenSuccessful() throws Exception {
    prepareServerResponse("this_is_bytecode");

    Artifactory underTest = getMavenArtifactory();

    File targetFile = temp.newFile();
    boolean found = underTest.downloadToFile(SONAR_PLUGIN_API, targetFile);

    assertThat(found).isTrue();
    assertThat(targetFile).exists().hasContent("this_is_bytecode");

    RecordedRequest request = mockWebServerRule.getServer().takeRequest();
    assertThat(request.getTarget()).isEqualTo("/org/sonarsource/sonarqube/sonar-plugin-api/3.0/sonar-plugin-api-3.0.jar");
    assertThat(request.getHeaders().get("Authorization")).isNull();
  }

  @Test
  public void resolveVersion_whenLATESTRELEASE_shouldResolveToHighestAvailableVersion() throws Exception {
    prepareServerResponse(getContent());
    Artifactory underTest = getMavenArtifactory();
    MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", "LATEST_RELEASE");

    Optional<String> version = underTest.resolveVersion(location);

    assertThat(version).hasValue("3.0.1.54424");
  }

  @Test
  public void resolveVersion_whenInvalidVersion_shouldThrowException() throws Exception {
    Artifactory underTest = getMavenArtifactory();

    for (String input : new String[] {"DEV", "LTS", "COMPATIBLE"}) {
      prepareServerResponse(getContent());
      MavenLocation location = MavenLocation.of("org.sonarsource.sonarqube", "sonar-plugin-api", input);
      assertThrows(IllegalStateException.class, () -> underTest.resolveVersion(location));
    }
  }

  private void prepareServerResponse(String content) {
    mockWebServerRule.getServer().enqueue(new MockResponse.Builder().body(content).build());
  }

  private MavenArtifactory getMavenArtifactory() throws IOException {
    return new MavenArtifactory(temp.newFolder(), mockWebServerRule.getServer().url("/").toString());
  }

}
