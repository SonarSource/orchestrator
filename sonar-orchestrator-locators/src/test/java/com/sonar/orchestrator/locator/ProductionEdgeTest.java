/*
 * Orchestrator Locators
 * Copyright (C) SonarSource Sàrl
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
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "VERIFY_PRODUCTION_EDGE", matches = "true")
class ProductionEdgeTest {

  @TempDir
  Path tempDir;

  @Test
  void downloads_private_distribution_and_resolves_release_alias() throws Exception {
    String token = System.getenv("ARTIFACTORY_ACCESS_TOKEN");
    assertTrue(token != null && !token.isBlank(), "A fresh Artifactory reader token is required");
    Configuration configuration = Configuration.builder()
      .setProperty("orchestrator.home", tempDir)
      .setProperty("ARTIFACTORY_URL", System.getenv("ARTIFACTORY_URL"))
      .setProperty("ARTIFACTORY_ACCESS_TOKEN", token)
      .build();

    Artifactory artifactory = ArtifactoryFactory.createArtifactory(configuration);
    assertThat(artifactory).isInstanceOf(DefaultArtifactory.class);
    assertThat(artifactory.getBaseUrl()).isEqualTo("https://repox-internal.sonar.build/artifactory");

    MavenLocation alias = privateDistribution("LATEST_RELEASE[2026.5.2]");
    assertThat(artifactory.resolveVersion(alias)).contains("2026.5.2.132813");

    MavenLocation release = privateDistribution("2026.5.2.132813");
    File distribution = tempDir.resolve(release.getFilename()).toFile();
    assertTrue(artifactory.downloadToFile(release, distribution), "Private distribution download failed");
    assertThat(distribution).isFile().isNotEmpty();
    try (ZipFile zip = new ZipFile(distribution)) {
      assertThat(zip.size()).isPositive();
    }
  }

  private static MavenLocation privateDistribution(String version) {
    return MavenLocation.builder()
      .setGroupId("com.sonarsource.sonarqube")
      .setArtifactId("sonarqube-developer")
      .setVersion(version)
      .withPackaging("zip")
      .build();
  }
}
