/*
 * Orchestrator Configuration
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
package com.sonar.orchestrator.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.getUserDirectory;

public class FileSystem {
  private final Path orchestratorHome;
  @Nullable
  private final Path mavenHome;
  private final Path mavenLocalRepository;
  private final Path sonarQubeZipsDir;
  private final Path workspace;
  @Nullable
  private final Path javaHome;
  @Nullable
  private final Path antHome;

  public FileSystem(Path homeDir, Configuration config) {
    this.orchestratorHome = homeDir;
    this.mavenHome = initDir(config, asList("maven.home", "MAVEN_HOME"), null);
    this.mavenLocalRepository = initDir(config, asList("maven.localRepository", "MAVEN_LOCAL_REPOSITORY"), getUserDirectory().toPath().resolve(".m2/repository"));
    this.sonarQubeZipsDir = initDir(config, singletonList("orchestrator.sonarInstallsDir"), orchestratorHome.resolve("zips"));
    this.workspace = initDir(config, singletonList("orchestrator.workspaceDir"), Path.of("target"));
    this.javaHome = initDir(config, asList("java.home", "JAVA_HOME"), null);
    this.antHome = initDir(config, asList("ant.home", "ANT_HOME"), null);
  }

  @CheckForNull
  private static Path initDir(Configuration config, List<String> propertyKeys, @Nullable Path defaultPath) {
    return propertyKeys.stream()
      .map(config::getString)
      .filter(Objects::nonNull)
      .findFirst()
      .map(Path::of)
      .orElse(defaultPath);
  }

  public Path mavenLocalRepository() {
    return mavenLocalRepository;
  }

  @CheckForNull
  public Path mavenHome() {
    return mavenHome;
  }

  @CheckForNull
  public Path antHome() {
    return antHome;
  }

  @CheckForNull
  public Path javaHome() {
    return javaHome;
  }

  public Path getOrchestratorHome() {
    return orchestratorHome;
  }

  public Path getCacheDir() {
    return orchestratorHome.resolve("cache");
  }

  public Path getSonarQubeZipsDir() {
    return sonarQubeZipsDir;
  }

  public Path workspace() {
    return workspace;
  }
}
