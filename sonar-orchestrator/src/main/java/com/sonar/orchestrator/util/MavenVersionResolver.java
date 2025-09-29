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
package com.sonar.orchestrator.util;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sonar.orchestrator.locator.MavenRepositoryVersion;
import com.sonar.orchestrator.version.Version;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;

public class MavenVersionResolver {

  private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

  private final String baseUrl;
  private final String groupId;
  private final String artifactId;
  private List<Version> versions;

  public MavenVersionResolver(String baseUrl, String groupId, String artifactId) {
    this.baseUrl = baseUrl;
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  public void loadVersions() {
    try {
      MavenRepositoryVersion mavenMetadata = downloadVersions();
      this.versions = mavenMetadata.getVersioning().getVersions()
        .stream()
        .map(Version::create)
        .collect(Collectors.toList());
      this.versions.sort(Comparator.naturalOrder());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to load versions of " + groupId + ":" + artifactId, e);
    }
  }

  public Optional<String> getLatestVersion(@Nullable String majorMinorVersion) {
    if (isEmpty(majorMinorVersion)) {
      Version version = this.versions.get(versions.size() - 1);
      return Optional.of(version.toString());
    }

    List<Version> filteredVersions = versions.stream()
      .filter(version -> version.toString().startsWith(majorMinorVersion))
      .collect(Collectors.toList());

    if (!filteredVersions.isEmpty()) {
      return filteredVersions.stream()
        .max(Comparator.naturalOrder())
        .map(Version::toString);
    } else {
      return Optional.empty();
    }
  }

  protected MavenRepositoryVersion downloadVersions() throws IOException {
    URL url = getUrl();
    return new XmlMapper().readValue(url, MavenRepositoryVersion.class);
  }

  @NotNull
  private URL getUrl() {
    return HttpUrl.parse(baseUrl).newBuilder()
      .addEncodedPathSegments(StringUtils.replace(groupId, ".", "/"))
      .addPathSegment(artifactId)
      .addPathSegment(MAVEN_METADATA_XML)
      .build()
      .url();
  }

  public static class Builder {
    String baseUrl;
    String groupId;
    String artifactId;

    public Builder setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder setGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder setArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public MavenVersionResolver build() {
      return new MavenVersionResolver(baseUrl, groupId, artifactId);
    }
  }

}
