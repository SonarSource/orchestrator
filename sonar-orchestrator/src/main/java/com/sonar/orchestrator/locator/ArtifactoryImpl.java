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
import com.eclipsesource.json.JsonValue;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpClientFactory;
import com.sonar.orchestrator.http.HttpException;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

public class ArtifactoryImpl implements Artifactory {

  private static final Logger LOG = LoggerFactory.getLogger(ArtifactoryImpl.class);

  private final File tempDir;
  private final String baseUrl;
  @Nullable
  private final String apiKey;

  public ArtifactoryImpl(File tempDir, String baseUrl, @Nullable String apiKey) {
    this.tempDir = tempDir;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
  }

  public static ArtifactoryImpl create(Configuration configuration) {
    File downloadTempDir = new File(configuration.fileSystem().workspace(), "temp-downloads");
    String baseUrl = defaultIfEmpty(configuration.getStringByKeys("orchestrator.artifactory.url", "ARTIFACTORY_URL"), "https://repox.jfrog.io/repox");
    String apiKey = configuration.getStringByKeys("orchestrator.artifactory.apiKey", "ARTIFACTORY_API_KEY");
    return new ArtifactoryImpl(downloadTempDir, baseUrl, apiKey);
  }

  @Override
  public boolean downloadToFile(MavenLocation location, File toFile) {
    Optional<File> tempFile = downloadToDir(location, tempDir);
    if (tempFile.isPresent()) {
      try {
        FileUtils.deleteQuietly(toFile);
        FileUtils.moveFile(tempFile.get(), toFile);
        return true;
      } catch (IOException e) {
        throw new IllegalStateException("Fail to move file " + toFile, e);
      }
    }
    return false;
  }

  @Override
  public Optional<File> downloadToDir(MavenLocation location, File toDir) {
    for (String repository : asList("sonarsource", "sonarsource-qa", "sonarsource-dogfood-builds")) {
      HttpUrl url = HttpUrl.parse(baseUrl).newBuilder()
        .addPathSegment(repository)
        .addEncodedPathSegments(StringUtils.replace(location.getGroupId(), ".", "/"))
        .addPathSegment(location.getArtifactId())
        .addPathSegment(location.getVersion())
        .addPathSegment(location.getFilename())
        .build();

      HttpCall call = newArtifactoryCall(url);
      try {
        LOG.info("Downloading {}", url);
        File toFile = call.downloadToDirectory(toDir);
        LOG.info("Found {} at {}", location, url);
        return Optional.of(toFile);
      } catch (HttpException e) {
        if (e.getCode() != HTTP_NOT_FOUND && e.getCode() != HTTP_UNAUTHORIZED && e.getCode() != HTTP_FORBIDDEN) {
          throw new IllegalStateException("Failed to request " + url, e);
        } else {
          String errorMessage;
          try {
            JsonArray errors = Json.parse(e.getBody()).asObject().get("errors").asArray();
            errorMessage = StreamSupport.stream(errors.spliterator(), false)
                    .map(item -> item.asObject().get("message").asString())
                    .collect(Collectors.joining(", "));
          } catch (Exception ignored) {
            errorMessage = "--- Failed to parse response body -- ";
          }
          LOG.warn("Could not download artifact from repository '{}': {} - {}",
                  repository,
                  e.getCode(),
                  errorMessage
          );
        }
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
    } else if (location.getVersion().startsWith("DOGFOOD")) {
      repositories = "sonarsource-dogfood-builds";
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
        .map(Version::create)
        .max(Comparator.naturalOrder())
        .map(Version::toString);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to request versions at " + url, e);
    }
  }

  private HttpCall newArtifactoryCall(HttpUrl url) {
    HttpCall call = HttpClientFactory.create().newCall(url);
    if (!isEmpty(apiKey)) {
      call.setHeader("X-JFrog-Art-Api", apiKey);
    }
    return call;
  }

  /**
   * Examples:
   * "LATEST_RELEASE" -> ""
   * "LATEST_RELEASE[7]" -> "7"
   * "LATEST_RELEASE[7.1]" -> "7.1"
   * "LATEST_RELEASE[7.1.2]" -> "7.1.2"
   */
  private static String extractVersionFromAlias(String s) {
    int start = s.indexOf('[');
    int end = s.indexOf(']');
    if (start >= 0 && end > start) {
      return s.substring(start + 1, end);
    }
    return "";
  }

}
