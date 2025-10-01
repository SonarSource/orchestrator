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
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemTest {

  @TempDir
  private Path homeDir;

  @Test
  void test_defaults() {
    FileSystem underTest = new FileSystem(homeDir, Configuration.create());
    Path userHome = FileUtils.getUserDirectory().toPath();

    // optional directories
    assertThat(underTest.javaHome()).isNull();
    assertThat(underTest.antHome()).isNull();
    assertThat(underTest.mavenHome()).isNull();

    verifySameDirs(underTest.mavenLocalRepository(), userHome.resolve(".m2/repository"));
    verifySameDirs(underTest.workspace(), Path.of("target"));
    verifySameDirs(underTest.getOrchestratorHome(), homeDir);
    verifySameDirs(underTest.getCacheDir(), homeDir.resolve("cache"));
    verifySameDirs(underTest.getSonarQubeZipsDir(), homeDir.resolve("zips"));
  }

  @Test
  void configure_java_home(@TempDir Path dir) {
    FileSystem underTest = new FileSystem(homeDir, Configuration.builder().setProperty("java.home", dir).build());

    verifySameDirs(underTest.javaHome(), dir);
  }

  @Test
  void configure_maven_home(@TempDir Path dir) {
    FileSystem underTest = new FileSystem(homeDir, Configuration.builder().setProperty("maven.home", dir).build());

    verifySameDirs(underTest.mavenHome(), dir);
  }

  @Test
  void configure_maven_local_repository(@TempDir Path dir) {
    FileSystem underTest = new FileSystem(homeDir, Configuration.builder().setProperty("maven.localRepository", dir).build());

    verifySameDirs(underTest.mavenLocalRepository(), dir);
  }

  private static void verifySameDirs(Path dir1, Path dir2) {
    assertThat(dir1.toAbsolutePath()).isEqualTo(dir2.toAbsolutePath());
  }
}
