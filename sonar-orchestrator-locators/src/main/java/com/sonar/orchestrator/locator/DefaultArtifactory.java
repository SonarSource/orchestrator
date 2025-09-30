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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.http.HttpCall;
import java.io.File;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.apache.maven.artifact.versioning.ComparableVersion;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class DefaultArtifactory extends Artifactory {

  protected DefaultArtifactory(File tempDir, String baseUrl, @Nullable String accessToken, @Nullable String apiKey) {
    super(tempDir, baseUrl, accessToken, apiKey);
  }

  protected static DefaultArtifactory create(Configuration configuration) {
    File downloadTempDir = new File(configuration.fileSystem().workspace().toFile(), "temp-downloads");
    String baseUrl = defaultIfEmpty(configuration.getStringByKeys("orchestrator.artifactory.url", "ARTIFACTORY_URL"), "https://repox.jfrog.io/repox");
    String apiKey = configuration.getStringByKeys("orchestrator.artifactory.apiKey", "ARTIFACTORY_API_KEY");
    String accessToken = configuration.getStringByKeys("orchestrator.artifactory.accessToken", "ARTIFACTORY_ACCESS_TOKEN");
    return new DefaultArtifactory(downloadTempDir, baseUrl, accessToken, apiKey);
  }

  @Override
  public boolean downloadToFile(MavenLocation location, File toFile) {
    Optional<File> tempFile = this.downloadToDir(location, tempDir);
    return tempFile.filter(file -> super.moveFile(file, toFile)).isPresent();
  }

  @Override
  public Optional<File> downloadToDir(MavenLocation location, File toDir) {
    for (String repository : asList("sonarsource", "sonarsource-qa")) {
      Optional<File> optionalFile = super.downloadToDir(location, toDir, repository);
      if (optionalFile.isPresent()) {
        return optionalFile;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> resolveVersion(MavenLocation location) {
    String repositories;
    if (location.getVersion().startsWith("LATEST_RELEASE")) {
      // only the artifacts that have been released
      repositories = "sonarsource-releases";
    } else if (location.getVersion().startsWith("DEV")) {
      // only the artifacts that have been promoted (master + release branches)
      repositories = "sonarsource-builds";
    } else if (location.getVersion().startsWith("LTS") || location.getVersion().contains("COMPATIBLE")) {
      throw new IllegalStateException("Unsupported version alias for " + location);
    } else {
      return Optional.of(location.getVersion());
    }

    HttpUrl url = HttpUrl.parse(baseUrl).newBuilder()
      .addPathSegments("api/search/versions")
      .addQueryParameter("g", location.getGroupId())
      .addQueryParameter("a", location.getArtifactId())
      .addQueryParameter("remote", "0")
      .addQueryParameter("repos", repositories)
      .addQueryParameter("v", extractVersionFromAlias(location.getVersion()) + "*")
      .build();

    HttpCall call = newArtifactoryCall(url);
    try {
      JsonValue json = Json.parse(call.execute().getBodyAsString());
      JsonArray results = json.asObject().get("results").asArray();
      return StreamSupport.stream(results.spliterator(), false)
        .map(result -> result.asObject().get("version").asString())
        .map(ComparableVersion::new)
        .max(Comparator.naturalOrder())
        .map(ComparableVersion::toString);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to request versions at " + url, e);
    }
  }

}
