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
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private File homeDir;

  @Before
  public void setUp() throws Exception {
    homeDir = temp.newFolder();
  }

  @Test
  public void test_defaults() throws Exception {
    FileSystem underTest = new FileSystem(homeDir, Configuration.create());
    File userHome = FileUtils.getUserDirectory();

    // optional directories
    assertThat(underTest.javaHome()).isNull();
    assertThat(underTest.antHome()).isNull();
    assertThat(underTest.mavenHome()).isNull();

    verifySameDirs(underTest.mavenLocalRepository(), new File(userHome, ".m2/repository"));
    verifySameDirs(underTest.workspace(), new File("target"));
    verifySameDirs(underTest.getOrchestratorHome(), homeDir);
    verifySameDirs(underTest.getCacheDir(), new File(homeDir, "cache"));
    verifySameDirs(underTest.getSonarQubeZipsDir(), new File(homeDir, "zips"));
  }

  @Test
  public void configure_java_home() throws Exception {
    File dir = temp.newFolder();
    FileSystem underTest = new FileSystem(homeDir, Configuration.builder().setProperty("java.home", dir.getCanonicalPath()).build());

    verifySameDirs(underTest.javaHome(), dir);
  }

  @Test
  public void configure_maven_home() throws Exception {
    File dir = temp.newFolder();
    FileSystem underTest = new FileSystem(homeDir, Configuration.builder().setProperty("maven.home", dir.getCanonicalPath()).build());

    verifySameDirs(underTest.mavenHome(), dir);
  }

  @Test
  public void configure_maven_local_repository() throws Exception {
    File dir = temp.newFolder();
    FileSystem underTest = new FileSystem(homeDir, Configuration.builder().setProperty("maven.localRepository", dir.getCanonicalPath()).build());

    verifySameDirs(underTest.mavenLocalRepository(), dir);
  }

  private static void verifySameDirs(File dir1, File dir2) throws IOException {
    assertThat(dir1.getCanonicalPath()).isEqualTo(dir2.getCanonicalPath());
  }
}
