/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

public class FileLocation implements Location {
  private static final String ENV_SHARED_DIR = "SONAR_IT_SOURCES";
  private static final String PROP_SHARED_DIR = "orchestrator.it_sources";

  private final File file;

  private FileLocation(File file) {
    this.file = file;
  }

  public static FileLocation of(File file) {
    return new FileLocation(file);
  }

  public static FileLocation of(String path) {
    return of(new File(path));
  }

  public static FileLocation of(URL url) {
    Preconditions.checkNotNull(url);
    return new FileLocation(FileUtils.toFile(url));
  }

  public static ResourceLocation ofClasspath(String pathStartingWithSlash) {
    return ResourceLocation.create(pathStartingWithSlash);
  }

  /**
   * File located in the shared directory defined by the system property orchestrator.it_sources or environment variable SONAR_IT_SOURCES.
   * Example : ofShared("javascript/performancing/pom.xml")
   *
   * @deprecated  Replaced by {@link com.sonar.orchestrator.Orchestrator#getFileLocationOfShared(java.lang.String)}
   *              which take into account orchestrator.properties
   */
  @Deprecated
  public static FileLocation ofShared(String relativePath) {
    String prop = System.getProperty(PROP_SHARED_DIR);
    return ofShared(relativePath, prop != null ? prop : System.getenv(ENV_SHARED_DIR));
  }

  /**
   * Example : ofShared("pom.xml","javascript/performancing")
   *
   * @deprecated  Replaced by {@link com.sonar.orchestrator.Orchestrator#getFileLocationOfShared(java.lang.String)}
   *              which take into account orchestrator.properties
   */
  @Deprecated
  @VisibleForTesting
  static FileLocation ofShared(String relativePath, String rootPath) {
    Preconditions.checkNotNull(rootPath, String.format("System property '%s' or environment variable '%s' is missing", PROP_SHARED_DIR, ENV_SHARED_DIR));
    File rootDir = new File(rootPath);
    Preconditions.checkState(rootDir.isDirectory() && rootDir.exists(),
      String.format("Please check the system property '%s' or the env variable '%s'. Directory does not exist: %s", PROP_SHARED_DIR, ENV_SHARED_DIR, rootDir));
    return of(new File(rootDir, relativePath));
  }

  public File getFile() {
    return file;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FileLocation that = (FileLocation) o;
    return file.equals(that.file);
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public String toString() {
    return file.toString();
  }
}
