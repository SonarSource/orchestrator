/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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
package com.sonar.orchestrator.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.locator.URLLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.updatecenter.common.Release;
import org.sonar.updatecenter.common.UpdateCenter;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

public class ServerZipFinder {

  private static final Logger LOG = LoggerFactory.getLogger(ServerZipFinder.class);

  private final FileSystem fs;
  private final UpdateCenter updateCenter;
  private final ServerZipCache cache;

  public ServerZipFinder(FileSystem fs, UpdateCenter updateCenter) {
    this(fs, updateCenter, new ServerZipCache(fs));
  }

  @VisibleForTesting
  ServerZipFinder(FileSystem fs, UpdateCenter updateCenter, ServerZipCache cache) {
    this.fs = fs;
    this.updateCenter = updateCenter;
    this.cache = cache;
  }

  /**
   * Warning - name of returned zip file can be inconsistent with requested version, for example
   * {@code find(Version.of("5.4-SNAPSHOT")).getName()} may equal {@code "5.4-build1234"}
   */
  public File find(SonarDistribution distrib) {
    Optional<File> localZip = distrib.getZipFile();
    if (localZip.isPresent()) {
      return localZip.get();
    }
    Version version = distrib.version().orElseThrow(() -> new IllegalStateException("Missing SonarQube version"));
    File cached = cache.get(version);
    if (cached != null) {
      return cached;
    }

    File mavenFile = findInMavenLocalRepository(version);
    if (mavenFile != null) {
      return mavenFile;
    }

    File download = downloadFromUpdateCenter(version);
    if (download != null) {
      return cache.moveToCache(version, download);
    }
    throw new IllegalStateException(format("SonarQube %s not found", version));
  }

  /**
   * Search for the zip in Maven local repository. If found then no need to copy to cache. It's already
   * on disk in a shared location that is persisted between executions.
   */
  @CheckForNull
  private File findInMavenLocalRepository(Version version) {
    // search for zip in maven repositories
    MavenLocation location = MavenLocation.builder()
      .setGroupId("org.codehaus.sonar")
      .setArtifactId("sonar-application")
      .setVersion(version)
      .withPackaging("zip")
      .build();
    File result = fs.locate(location);
    if (result == null) {
      location = MavenLocation.builder()
        .setGroupId("org.sonarsource.sonarqube")
        .setArtifactId("sonar-application")
        .setVersion(version)
        .withPackaging("zip")
        .build();
      result = fs.locate(location);
    }
    if (result != null) {
      LOG.info("SonarQube {} found in Maven local repository [{}]", version, result);
    } else {
      LOG.info("SonarQube {} not found in Maven local repository [{}]", version, fs.mavenLocalRepository());
    }
    return result;
  }

  @CheckForNull
  private File downloadFromUpdateCenter(Version version) {
    String url = getDownloadUrl(version);
    if (isBlank(url)) {
      LOG.info("SonarQube {} is not defined in update center", version);
      return null;
    }

    try {
      File tempDir = Files.createTempDir();
      return fs.copyToDirectory(URLLocation.create(new URL(url)), tempDir);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to download " + url, e);
    }
  }

  @CheckForNull
  private String getDownloadUrl(Version version) {
    for (Release release : updateCenter.getSonar().getAllReleases()) {
      if (release.getVersion().getName().equals(version.toString())) {
        return release.getDownloadUrl();
      }
    }
    return null;
  }
}
