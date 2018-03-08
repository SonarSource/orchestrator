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
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpClientFactory;
import com.sonar.orchestrator.http.HttpException;
import java.io.File;
import java.io.IOException;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

class Artifactory {

  private static final Logger LOG = LoggerFactory.getLogger(Artifactory.class);

  private final Configuration configuration;

  Artifactory(Configuration configuration) {
    this.configuration = configuration;
  }

  public boolean downloadToFile(MavenLocation location, File toFile) {
    String baseUrl = defaultIfEmpty(configuration.getStringByKeys("orchestrator.artifactory.url", "ARTIFACTORY_URL"), "https://repox.sonarsource.com");
    String repositories = defaultIfEmpty(configuration.getStringByKeys("orchestrator.artifactory.repositories", "ARTIFACTORY_REPOSITORIES"), "sonarsource");
    File downloadTempDir = new File(configuration.fileSystem().workspace(), "temp-downloads");

    for (String repository : repositories.split(",")) {
      HttpUrl url = HttpUrl.parse(baseUrl).newBuilder()
        .addPathSegment(repository)
        .addEncodedPathSegments(StringUtils.replace(location.getGroupId(), ".", "/"))
        .addPathSegment(location.getArtifactId())
        .addPathSegment(location.version().toString())
        .addPathSegment(location.getFilename())
        .build();

      HttpCall call = newArtifactoryCall(url);
      try {
        LOG.info("Downloading {}", url);
        File tempFile = call.downloadToDirectory(downloadTempDir);
        FileUtils.deleteQuietly(toFile);
        FileUtils.moveFile(tempFile, toFile);
        LOG.info("Found {} at {}", location, url);
        return true;
      } catch (HttpException e) {
        if (e.getCode() != 404 && e.getCode() != 401 && e.getCode() != 403) {
          throw new IllegalStateException("Failed to request " + url, e);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Fail to download " + url + " into " + toFile, e);
      }
    }
    return false;
  }

  private HttpCall newArtifactoryCall(HttpUrl url) {
    HttpCall call = HttpClientFactory.create().newCall(url);
    String apiKey = configuration.getStringByKeys("orchestrator.artifactory.apiKey", "ARTIFACTORY_API_KEY");
    if (!isEmpty(apiKey)) {
      call.setHeader("X-JFrog-Art-Api", apiKey);
    }
    return call;
  }
}
