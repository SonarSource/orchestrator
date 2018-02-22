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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.config.Configuration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenLocator implements Locator<MavenLocation> {
  private static final Logger LOG = LoggerFactory.getLogger(MavenLocator.class);

  private final Configuration config;
  private final Artifactory artifactory;

  public MavenLocator(Configuration config) {
    this(config, new Artifactory(config));
  }

  MavenLocator(Configuration config, Artifactory artifactory) {
    this.config = config;
    this.artifactory = artifactory;
  }

  @Override
  public File locate(MavenLocation location) {
    // 1. check cache
    String cacheKey = cacheKeyOf(location);
    File cachedDir = new File(config.fileSystem().getCacheDir(), cacheKey);
    if (cachedDir.exists()) {
      Collection<File> files = FileUtils.listFiles(cachedDir, null, false);
      if (files.size() == 1) {
        File file = files.iterator().next();
        LOG.info("Found {} at {}", location, file);
        return file;
      }
    }

    // 2. check Maven local repository, if defined.
    File file = locateInLocalRepository(location);
    if (file != null) {
      LOG.info("Found {} in Maven local repository at {}", location, file);
      return file;
    }

    // 3. download from Artifactory, if defined.
    // No need to try if SNAPSHOT, only releases are deployed.
    if (location.version().isSnapshot()) {
      return null;
    }
    File cachedFile = new File(cachedDir, location.getFilename());
    boolean found = artifactory.downloadToFile(location, cachedFile);
    return found ? cachedFile : null;
  }

  @CheckForNull
  private File locateInLocalRepository(MavenLocation location) {
    if (config.fileSystem().mavenLocalRepository() != null && config.fileSystem().mavenLocalRepository().exists()) {
      File file = new File(config.fileSystem().mavenLocalRepository(), pathInMavenLocalRepository(location));
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
    return StringUtils.replace(location.getGroupId(), ".", "/") + "/" + location.getArtifactId() + "/" + location.version() + "/" + location.getFilename();
  }

  private static String cacheKeyOf(MavenLocation location) {
    return DigestUtils.md5Hex(location.toString());
  }
}
