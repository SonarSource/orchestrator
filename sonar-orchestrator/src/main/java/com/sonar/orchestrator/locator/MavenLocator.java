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
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class MavenLocator implements Locator<MavenLocation> {
  private static final Logger LOG = LoggerFactory.getLogger(MavenLocator.class);

  private final Configuration config;

  public MavenLocator(Configuration config) {
    this.config = config;
  }

  @Override
  public File locate(MavenLocation location) {
    return locateInLocalRepositoryAndLog(location);
  }

  static URL appendPathToUrl(String path, URL baseUrl) throws MalformedURLException {
    return new URL(StringUtils.removeEnd(baseUrl.toString(), "/") + "/" + path);
  }

  @CheckForNull
  private File locateInLocalRepository(MavenLocation location) {
    if (config.fileSystem().mavenLocalRepository() != null && config.fileSystem().mavenLocalRepository().exists()) {
      File file = new File(config.fileSystem().mavenLocalRepository(), path(location));
      if (file.exists()) {
        return file;
      }
    }
    return null;
  }

  @CheckForNull
  private File locateInLocalRepositoryAndLog(MavenLocation location) {
    File result = locateInLocalRepository(location);
    if (result != null) {
      LOG.info("Found {} in maven local repository at {}", location, result);
    }
    return result;
  }

  @Override
  public File copyToDirectory(MavenLocation location, File toDir) {
    File target = locateInLocalRepositoryAndLog(location);
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
    File target = locateInLocalRepositoryAndLog(location);
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
    File target = locateInLocalRepositoryAndLog(location);
    if (target == null) {
      return null;
    }

    return openFromLocalRepository(target);
  }

  private static InputStream openFromLocalRepository(File target) {
    try {
      return FileUtils.openInputStream(target);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to open " + target, e);
    }
  }

  private static String path(MavenLocation location) {
    return StringUtils.replace(location.getGroupId(), ".", "/") + "/" + location.getArtifactId() + "/" + location.version() + "/" + location.getFilename();
  }
}
