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

import com.sonar.orchestrator.config.Configuration;
import java.io.File;

import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfEmpty;

public class ArtifactoryFactory {

  private static final String DEFAULT_ARTIFACTORY_PREFIX = "https://repox.jfrog.io";
  private static final String DEFAULT_ARTIFACTORY_URL = DEFAULT_ARTIFACTORY_PREFIX + "/repox";

  /**
   * Two types of Artifactory are supported: Maven and Default.
   *
   * <p>
   * Default repox artifactory is used if orchestrator.artifactory.url is empty or specifically set to <a href="https://repox.jfrog.io/repox">repox</a>.
   * Otherwise, we assume the URL points to a maven repository.
   * </p>
   */
  public static Artifactory createArtifactory(Configuration configuration) {
    File downloadTempDir = new File(configuration.fileSystem().workspace(), "temp-downloads");
    String baseUrl = defaultIfEmpty(configuration.getStringByKeys("orchestrator.artifactory.url", "ARTIFACTORY_URL"), DEFAULT_ARTIFACTORY_URL);

    if (baseUrl.startsWith(DEFAULT_ARTIFACTORY_PREFIX)) {
      String accessToken = configuration.getStringByKeys("orchestrator.artifactory.accessToken", "ARTIFACTORY_ACCESS_TOKEN");
      String apiKey = configuration.getStringByKeys("orchestrator.artifactory.apiKey", "ARTIFACTORY_API_KEY");
      return new DefaultArtifactory(downloadTempDir, baseUrl, accessToken, apiKey);
    } else {
      return new MavenArtifactory(downloadTempDir, baseUrl);
    }
  }

  private ArtifactoryFactory() {
  }

}
