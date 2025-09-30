/*
 * Orchestrator Locators
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class MavenVersionResolverTest {

  @ParameterizedTest
  @MethodSource("params")
  void loadVersions(String input, String output) throws IOException {
    MavenVersionResolver versionResolver = spy(new MavenVersionResolver("https://repo1.maven.org/maven2", "org.sonarsource.sonarqube", "sonar-plugin-api"));
    when(versionResolver.downloadVersions()).thenReturn(getMavenRepositoryVersion());
    versionResolver.loadVersions();

    Optional<String> version = versionResolver.getLatestVersion(input);
    Assertions.assertTrue(version.isPresent());
    Assertions.assertEquals(output, version.get());
  }

  @Test
  void loadVersions_whenBadUrl_shouldThrowException() {
    MavenVersionResolver versionResolver = spy(new MavenVersionResolver("https://www.iamabadurl.com", "can-never-exist", "period"));
    assertThrows(IllegalStateException.class, versionResolver::loadVersions);
  }

  @Test
  void builder_shouldSetFields() {
    MavenVersionResolver.Builder versionResolver = new MavenVersionResolver.Builder()
      .setBaseUrl("https://repo1.maven.org/maven2")
      .setGroupId("org.sonarsource.sonarqube")
      .setArtifactId("sonar-plugin-api");

    Assertions.assertNotNull(versionResolver);
    Assertions.assertEquals("https://repo1.maven.org/maven2", versionResolver.baseUrl);
    Assertions.assertEquals("org.sonarsource.sonarqube", versionResolver.groupId);
    Assertions.assertEquals("sonar-plugin-api", versionResolver.artifactId);
  }

  MavenRepositoryVersion getMavenRepositoryVersion() {
    MavenRepositoryVersion mavenRepositoryVersion = new MavenRepositoryVersion();
    MavenRepositoryVersion.Versioning versioning = new MavenRepositoryVersion.Versioning();
    List<String> versions = new ArrayList<>();
    versions.add("1.0");
    versions.add("2.0");
    versions.add("2.0.1");
    versions.add("2.0.2");
    versions.add("2.0.10");
    versions.add("10.9.1");
    versions.add("11.4.5");
    versions.add("11.0.3");
    versions.add("10.4.2");
    versions.add("10.10.0");
    versions.add("3.1");
    versions.add("4.0");
    versions.add("5.0");
    versions.add("5.0.1");
    versions.add("5.0.1.9324");
    versions.add("6.0");
    versioning.setVersions(versions);
    mavenRepositoryVersion.setVersioning(versioning);
    return mavenRepositoryVersion;
  }

  private static Stream<Arguments> params() {
    return Stream.of(
      Arguments.of("", "11.4.5"),
      Arguments.of("10", "10.10.0"),
      Arguments.of("5.", "5.0.1.9324"),
      Arguments.of("2.0", "2.0.10"),
      Arguments.of("5.0.1", "5.0.1.9324")
    );
  }

}
