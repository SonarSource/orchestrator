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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

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
    requireNonNull(url);
    return new FileLocation(FileUtils.toFile(url));
  }

  public static ResourceLocation ofClasspath(String pathStartingWithSlash) {
    return ResourceLocation.create(pathStartingWithSlash);
  }

  /**
   * Get a file which name matches a wildcard pattern. The specified directory must exist. Sub-directories
   * are not traversed. The wildcard pattern does not apply to directory path.
   *
   * Example: {@code byWildcardFilename(new File("../target"), "sonar-foo-plugin-*.jar")}
   *
   * @since 3.9
   * @throws IllegalStateException if the directory does not exist
   */
  public static FileLocation byWildcardFilename(File directory, String wildcardFilename) {
    if (!directory.exists()) {
      throw new IllegalStateException(format("Directory [%s] does not exist", directory));
    }
    WildcardFileFilter filter = new WildcardFileFilter(wildcardFilename, IOCase.SENSITIVE);
    Collection<File> files = new ArrayList<>(FileUtils.listFiles(directory, filter, null));
    return getOnlyFile(directory, wildcardFilename, files);
  }

  /**
   * Same as {@link #byWildcardFilename(File, String)} except that some Maven files
   * are excluded from search:
   * <ul>
   *   <li>files matching *-sources.jar</li>
   *   <li>files matching *-tests.jar</li>
   *   <li>files matching *-javadoc.jar</li>
   * </ul>
   * Example: {@code byWildcardMavenArtifactFilename(new File("../target"), "sonar-foo-plugin-*.jar")}
   *
   * @since 3.9
   * @throws IllegalStateException if the directory does not exist
   */
  public static FileLocation byWildcardMavenFilename(File directory, String wildcardFilename) {
    if (!directory.exists()) {
      throw new IllegalStateException(format("Directory [%s] does not exist", directory));
    }
    IOFileFilter artifactFilter = new WildcardFileFilter(wildcardFilename, IOCase.SENSITIVE);
    IOFileFilter sourcesFilter = notFileFilter(new WildcardFileFilter("*-sources.jar"));
    IOFileFilter testsFilter = notFileFilter(new WildcardFileFilter("*-tests.jar"));
    IOFileFilter javadocFilter = notFileFilter(new WildcardFileFilter("*-javadoc.jar"));
    IOFileFilter filters = and(artifactFilter, sourcesFilter, testsFilter, javadocFilter);
    Collection<File> files = new ArrayList<>(FileUtils.listFiles(directory, filters, null));
    return getOnlyFile(directory, wildcardFilename, files);
  }

  private static FileLocation getOnlyFile(File directory, String wildcardFilename, Collection<File> files) {
    if (files.isEmpty()) {
      throw new IllegalStateException(format("No files match [%s] in directory [%s]", wildcardFilename, directory));
    }
    if (files.size() > 1) {
      String filenames = files.stream().map(File::getName).collect(Collectors.joining(", "));
      throw new IllegalStateException(format("Multiple files match [%s] in directory [%s]: %s", wildcardFilename, directory, filenames));
    }
    return of(files.iterator().next());
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
  static FileLocation ofShared(String relativePath, String rootPath) {
    requireNonNull(rootPath, format("System property '%s' or environment variable '%s' is missing", PROP_SHARED_DIR, ENV_SHARED_DIR));
    File rootDir = new File(rootPath);
    checkState(rootDir.isDirectory() && rootDir.exists(),
      "Please check the system property '%s' or the env variable '%s'. Directory does not exist: %s", PROP_SHARED_DIR, ENV_SHARED_DIR, rootDir);
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
