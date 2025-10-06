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
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpClientFactory;
import com.sonar.orchestrator.http.HttpException;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public abstract class Artifactory {

  private static final Logger LOG = LoggerFactory.getLogger(Artifactory.class);

  protected final File tempDir;
  protected final String baseUrl;
  @Nullable
  protected final String apiKey;
  @Nullable
  protected final String accessToken;

  protected Artifactory(File tempDir, String baseUrl, @Nullable String accessToken, @Nullable String apiKey) {
    this.tempDir = tempDir;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.accessToken = accessToken;
  }

  /**
   * Examples:
   * "LATEST_RELEASE" -> ""
   * "LATEST_RELEASE[7]" -> "7"
   * "LATEST_RELEASE[7.1]" -> "7.1"
   * "LATEST_RELEASE[7.1.2]" -> "7.1.2"
   */
  protected static String extractVersionFromAlias(String s) {
    int start = s.indexOf('[');
    int end = s.indexOf(']');
    if (start >= 0 && end > start) {
      return s.substring(start + 1, end);
    }
    return "";
  }

  protected boolean moveFile(File tempFile, File toFile) {
    try {
      FileUtils.deleteQuietly(toFile);
      FileUtils.moveFile(tempFile, toFile);
      return true;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to move file " + toFile, e);
    }
  }

  protected Optional<File> downloadToDir(MavenLocation location, File toDir, @Nullable String repository) {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
    if (!isEmpty(repository)) {
      urlBuilder.addPathSegment(repository);
    }
    HttpUrl url = urlBuilder.addEncodedPathSegments(Strings.CS.replace(location.getGroupId(), ".", "/"))
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
          errorMessage);
      }
    }
    return Optional.empty();
  }

  protected HttpCall newArtifactoryCall(HttpUrl url) {
    HttpCall call = HttpClientFactory.create().newCall(url);
    if (!isEmpty(accessToken)) {
      call.setHeader("Authorization", "Bearer " + accessToken);
    } else if (!isEmpty(apiKey)) {
      call.setHeader("X-JFrog-Art-Api", apiKey);
    }
    return call;
  }

  protected String getBaseUrl() {
    return baseUrl;
  }

  protected String getApiKey() {
    return apiKey;
  }

  protected String getAccessToken() {
    return accessToken;
  }

  public abstract Optional<String> resolveVersion(MavenLocation location);

  public abstract boolean downloadToFile(MavenLocation location, File toFile);

  public abstract Optional<File> downloadToDir(MavenLocation location, File toDir);

}
