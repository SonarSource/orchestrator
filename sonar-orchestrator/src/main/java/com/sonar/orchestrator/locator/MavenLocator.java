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

import com.sonar.orchestrator.config.FileSystem;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenLocator implements Locator<MavenLocation> {
  private static final Logger LOG = LoggerFactory.getLogger(MavenLocator.class);

  private final FileSystem fileSystem;
  private final Artifactory artifactory;

  public MavenLocator(FileSystem fileSystem, Artifactory artifactory) {
    this.fileSystem = fileSystem;
    this.artifactory = artifactory;
  }

  @Override
  public File locate(MavenLocation location) {
    // resolve the version alias if needed (requires to be online)
    MavenLocation resolvedLocation = resolveLocation(location);
    return locateResolvedVersion(resolvedLocation);
  }

  @Nullable
  File locateResolvedVersion(MavenLocation resolvedLocation) {
    // check local cache
    String cacheKey = cacheKeyOf(resolvedLocation);
    File cachedDir = new File(fileSystem.getCacheDir(), cacheKey);
    if (cachedDir.exists()) {
      Collection<File> files = FileUtils.listFiles(cachedDir, null, false);
      if (files.size() == 1) {
        File file = files.iterator().next();
        LOG.info("Found {} at {}", resolvedLocation, file);
        return file;
      }
    }

    // check Maven local repository, if defined.
    File file = locateInLocalRepository(resolvedLocation);
    if (file != null) {
      LOG.info("Found {} in Maven local repository at {}", resolvedLocation, file);
      return file;
    }

    // download from Artifactory.
    // No need to try if SNAPSHOT, only releases are deployed.
    if (resolvedLocation.getVersion().endsWith("-SNAPSHOT")) {
      return null;
    }
    File cachedFile = new File(cachedDir, resolvedLocation.getFilename());
    boolean found = artifactory.downloadToFile(resolvedLocation, cachedFile);
    return found ? cachedFile : null;
  }

  private MavenLocation resolveLocation(MavenLocation location) {
    Optional<String> version = resolveVersion(location);
    if (!version.isPresent()) {
      throw new IllegalStateException("Version can not be resolved: " + location);
    }
    if (version.get().equals(location.getVersion())) {
      return location;
    }
    return MavenLocation.builder()
      .setGroupId(location.getGroupId())
      .setArtifactId(location.getArtifactId())
      .setClassifier(location.getClassifier())
      .withPackaging(location.getPackaging())
      .setVersion(version.get())
      .build();
  }

  public Optional<String> resolveVersion(MavenLocation location) {
    return artifactory.resolveVersion(location);
  }

  @CheckForNull
  private File locateInLocalRepository(MavenLocation location) {
    if (fileSystem.mavenLocalRepository() != null && fileSystem.mavenLocalRepository().exists()) {
      File file = new File(fileSystem.mavenLocalRepository(), pathInMavenLocalRepository(location));
      if (file.exists()) {
        return file;
      }
    }
    return null;
  }

  @Override
  public File copyToDirectory(MavenLocation location, File toDir) {
    File target = locate(location);
    if (target == null) {
      return null;
    }

    try {
      FileUtils.copyFileToDirectory(target, toDir);
      return new File(toDir, target.getName());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy file to dir: " + toDir, e);
    }
  }

  @Override
  public File copyToFile(MavenLocation location, File toFile) {
    File target = locate(location);
    if (target == null) {
      return null;
    }

    try {
      FileUtils.copyFile(target, toFile);
      return toFile;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to copy to file: " + toFile, e);
    }
  }

  @Override
  @CheckForNull
  public InputStream openInputStream(MavenLocation location) {
    File target = locate(location);
    if (target == null) {
      return null;
    }

    try {
      return FileUtils.openInputStream(target);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open " + target, e);
    }
  }

  private static String pathInMavenLocalRepository(MavenLocation location) {
    return StringUtils.replace(location.getGroupId(), ".", "/") + "/" + location.getArtifactId() + "/" + location.getVersion() + "/" + location.getFilename();
  }

  static String cacheKeyOf(MavenLocation location) {
    return DigestUtils.md5Hex(location.toString());
  }
}
