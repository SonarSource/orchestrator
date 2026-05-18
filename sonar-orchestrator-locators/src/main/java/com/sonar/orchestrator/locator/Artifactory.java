/*
 * Orchestrator Locators
 * Copyright (C) 2011-2025 SonarSource Sàrl
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
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

  /**
   * Atomically move {@code source} into {@code target}, tolerating a concurrent winner.
   * <p>
   * Readers (see {@link MavenLocator#locateResolvedVersion}) list the cache directory and return its single
   * file: an atomic rename guarantees the cached file appears fully written or not at all.
   */
  protected void moveFile(Path source, Path target) {
    try {
      Files.createDirectories(target.getParent());
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e1) {
      if (Files.isRegularFile(target)) {
        LOG.debug("File {} was cached concurrently; discarding our copy {}", target, source);
        return;
      }
      LOG.warn("Atomic move from {} to {} failed: {}", source, target, e1.getMessage());
      LOG.warn("Falling back to a non-atomic move");
      try {
        Files.move(source, target);
      } catch (FileAlreadyExistsException e2) {
        LOG.debug("File {} was cached concurrently during fallback; discarding our copy {}", target, source);
      } catch (IOException e2) {
        throw new IllegalStateException("Fail to move file " + target, e2);
      }
    }
  }

  /**
   * Download {@code location} from {@code repository} into {@code destination}. The destination filename is
   * controlled by the caller (not derived from HTTP {@code Content-Disposition}), so callers can supply a
   * unique temp file path safe to use from multiple JVMs.
   */
  protected boolean downloadFromRepository(MavenLocation location, Path destination, @Nullable String repository) {
    HttpUrl url = buildArtifactUrl(location, repository);
    HttpCall call = newArtifactoryCall(url);
    try {
      LOG.info("Downloading {}", url);
      call.downloadToFile(destination.toFile());
      LOG.info("Found {} at {}", location, url);
      return true;
    } catch (HttpException e) {
      handleDownloadFailure(e, url, repository);
      return false;
    }
  }

  private HttpUrl buildArtifactUrl(MavenLocation location, @Nullable String repository) {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
    if (!isEmpty(repository)) {
      urlBuilder.addPathSegment(repository);
    }
    return urlBuilder.addEncodedPathSegments(Strings.CS.replace(location.getGroupId(), ".", "/"))
      .addPathSegment(location.getArtifactId())
      .addPathSegment(location.getVersion())
      .addPathSegment(location.getFilename())
      .build();
  }

  private static void handleDownloadFailure(HttpException e, HttpUrl url, @Nullable String repository) {
    if (e.getCode() != HTTP_NOT_FOUND && e.getCode() != HTTP_UNAUTHORIZED && e.getCode() != HTTP_FORBIDDEN) {
      throw new IllegalStateException("Failed to request " + url, e);
    }
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

  /**
   * Download {@code location} into {@code toFile}, atomically and safely from multiple JVMs.
   * <p>
   * Each call downloads into a unique temp file under {@link #tempDir} and, on success, atomically moves
   * it into place. A concurrent JVM landing the same file in the cache first is treated as success.
   */
  public boolean downloadToFile(MavenLocation location, File toFile) {
    Path tempFile;
    try {
      Files.createDirectories(tempDir.toPath());
      tempFile = Files.createTempFile(tempDir.toPath(), location.getFilename() + "-", ".tmp");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create temp file under " + tempDir, e);
    }
    try {
      if (!doDownload(location, tempFile)) {
        return false;
      }
      moveFile(tempFile, toFile.toPath());
      return true;
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException e) {
        LOG.debug("Could not delete temp file {}: {}", tempFile, e.getMessage());
      }
    }
  }

  /**
   * Fetch the artifact bytes into {@code destination}. Subclasses choose which Artifactory repository (or
   * repositories) to try. Returns {@code true} on success.
   */
  protected abstract boolean doDownload(MavenLocation location, Path destination);

}
