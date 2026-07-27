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
import java.util.List;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class ArtifactoryFactory {

  private static final String DEFAULT_ARTIFACTORY_PREFIX = "https://repox.jfrog.io";
  private static final String DEFAULT_ARTIFACTORY_URL = DEFAULT_ARTIFACTORY_PREFIX + "/artifactory";

  /**
   * Hosts that speak Artifactory REST APIs and require SonarSource credentials
   * ({@code ARTIFACTORY_ACCESS_TOKEN} / {@code orchestrator.artifactory.accessToken}).
   */
  private static final List<String> SONARSOURCE_ARTIFACTORY_PREFIXES = List.of(
    DEFAULT_ARTIFACTORY_PREFIX,
    "https://repox-internal.dev.sonar.build"
  );

  /**
   * Two types of Artifactory are supported: Maven and Default.
   *
   * <p>
   * Authenticated {@link DefaultArtifactory} is used when {@code orchestrator.artifactory.url} is empty
   * or points at a known SonarSource Artifactory host
   * ({@code https://repox.jfrog.io} or {@code https://repox-internal.dev.sonar.build}).
   * Otherwise, we assume the URL points to a plain Maven repository and use unauthenticated
   * {@link MavenArtifactory}.
   * </p>
   */
  public static Artifactory createArtifactory(Configuration configuration) {
    File downloadTempDir = configuration.fileSystem().getTempDir().toFile();
    String baseUrl = defaultIfEmpty(configuration.getStringByKeys("orchestrator.artifactory.url", "ARTIFACTORY_URL"), DEFAULT_ARTIFACTORY_URL);

    if (isSonarSourceArtifactory(baseUrl)) {
      String accessToken = configuration.getStringByKeys("orchestrator.artifactory.accessToken", "ARTIFACTORY_ACCESS_TOKEN");
      String apiKey = configuration.getStringByKeys("orchestrator.artifactory.apiKey", "ARTIFACTORY_API_KEY");
      return new DefaultArtifactory(downloadTempDir, baseUrl, accessToken, apiKey);
    } else {
      return new MavenArtifactory(downloadTempDir, baseUrl);
    }
  }

  private static boolean isSonarSourceArtifactory(String baseUrl) {
    okhttp3.HttpUrl url = okhttp3.HttpUrl.parse(baseUrl);
    if (url == null) {
      return false;
    }

    String host = url.host();
    return SONARSOURCE_ARTIFACTORY_PREFIXES.stream()
      .map(okhttp3.HttpUrl::parse)
      .filter(java.util.Objects::nonNull)
      .anyMatch(allowed -> allowed.host().equals(host));
  }

  private ArtifactoryFactory() {
  }

}
