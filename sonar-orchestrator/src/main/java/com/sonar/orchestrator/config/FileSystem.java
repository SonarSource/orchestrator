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
package com.sonar.orchestrator.config;

import java.io.File;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.getUserDirectory;

public class FileSystem {
  private File orchestratorHome;
  @Nullable
  private File mavenHome;
  private File mavenLocalRepository;
  private File sonarQubeZipsDir;
  private File workspace;
  @Nullable
  private File javaHome;
  @Nullable
  private File antHome;

  public FileSystem(File homeDir, Configuration config) {
    this.orchestratorHome = homeDir;
    this.mavenHome = initDir(config, asList("maven.home", "MAVEN_HOME"), null);
    this.mavenLocalRepository = initDir(config, asList("maven.localRepository", "MAVEN_LOCAL_REPOSITORY"), new File(getUserDirectory(), ".m2/repository"));
    this.sonarQubeZipsDir = initDir(config, singletonList("orchestrator.sonarInstallsDir"), new File(orchestratorHome, "zips"));
    this.workspace = initDir(config, singletonList("orchestrator.workspaceDir"), new File("target"));
    this.javaHome = initDir(config, asList("java.home", "JAVA_HOME"), null);
    this.antHome = initDir(config, asList("ant.home", "ANT_HOME"), null);
  }

  public File mavenLocalRepository() {
    return mavenLocalRepository;
  }

  @CheckForNull
  public File mavenHome() {
    return mavenHome;
  }

  @CheckForNull
  public File antHome() {
    return antHome;
  }

  @CheckForNull
  public File javaHome() {
    return javaHome;
  }

  public File getOrchestratorHome() {
    return orchestratorHome;
  }

  public File getCacheDir() {
    return new File(orchestratorHome, "cache");
  }

  public File getSonarQubeZipsDir() {
    return sonarQubeZipsDir;
  }

  public File workspace() {
    return workspace;
  }

  @CheckForNull
  private static File initDir(Configuration config, List<String> propertyKeys, @Nullable File defaultFile) {
    return propertyKeys.stream()
      .map(config::getString)
      .filter(Objects::nonNull)
      .findFirst()
      .map(File::new)
      .orElse(defaultFile);
  }
}
