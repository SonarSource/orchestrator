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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.http.HttpCall;
import com.sonar.orchestrator.http.HttpClientFactory;
import com.sonar.orchestrator.http.HttpException;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import okhttp3.HttpUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

public class ArtifactoryImpl implements Artifactory {

  private static final Logger LOG = LoggerFactory.getLogger(ArtifactoryImpl.class);

  private final Configuration configuration;

  public ArtifactoryImpl(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public boolean downloadToFile(MavenLocation location, File toFile) {
    String baseUrl = getBaseUrl();
    File downloadTempDir = new File(configuration.fileSystem().workspace(), "temp-downloads");

    for (String repository : asList("sonarsource", "sonarsource-qa")) {
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

    HttpUrl url = HttpUrl.parse(getBaseUrl()).newBuilder()
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
        .map(Version::new)
        .max(Comparator.naturalOrder())
        .map(v -> v.asString);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to request versions at " + url, e);
    }
  }

  private String getBaseUrl() {
    return defaultIfEmpty(configuration.getStringByKeys("orchestrator.artifactory.url", "ARTIFACTORY_URL"), "https://repox.sonarsource.com");
  }

  private HttpCall newArtifactoryCall(HttpUrl url) {
    HttpCall call = HttpClientFactory.create().newCall(url);
    String apiKey = configuration.getStringByKeys("orchestrator.artifactory.apiKey", "ARTIFACTORY_API_KEY");
    if (!isEmpty(apiKey)) {
      call.setHeader("X-JFrog-Art-Api", apiKey);
    }
    return call;
  }

  /**
   * Examples:
   * "LATEST_RELEASE" -> ""
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

  static class Version implements Comparable<Version> {
    private final String asString;
    private final long asNumber;
    private final String qualifier;

    Version(String s) {
      String[] fields = StringUtils.substringBeforeLast(s, "-").split("\\.");
      long l = 0;
      // max representation: 9999.9999.9999.999999
      if (fields.length > 0) {
        l += 1_0000_0000_000000L * Integer.parseInt(fields[0]);
        if (fields.length > 1) {
          l += 1_0000_000000L * Integer.parseInt(fields[1]);
          if (fields.length > 2) {
            l += 1_000000L * Integer.parseInt(fields[2]);
            if (fields.length > 3) {
              l += Integer.parseInt(fields[3]);
            }
          }
        }
      }
      this.asNumber = l;
      this.asString = s;
      this.qualifier = s.contains("-") ? StringUtils.substringAfterLast(s, "-") : "ZZZ";
    }

    String asString() {
      return asString;
    }

    long asNumber() {
      return asNumber;
    }

    String qualifier() {
      return qualifier;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Version version = (Version) o;
      return asNumber == version.asNumber && Objects.equals(qualifier, version.qualifier);
    }

    @Override
    public int hashCode() {
      return Objects.hash(asNumber, qualifier);
    }

    @Override
    public int compareTo(Version o) {
      int i = Long.compare(asNumber, o.asNumber);
      if (i ==0) {
        i = qualifier.compareTo(o.qualifier);
      }
      return i;
    }
  }

}
