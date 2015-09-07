/*
 * Orchestrator
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.orchestrator.config;

import com.sonar.orchestrator.junit.PropertyFilterRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(PropertyFilterRunner.class)
public class FileSystemTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldLoadJavaHome() {
    File javaHome = FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/config/FileSystemTest/java"));
    Configuration config = Configuration.builder().setProperty("java.home", javaHome).build();
    FileSystem fileSystem = new FileSystem(config);

    assertThat(fileSystem.javaHome()).isEqualTo(javaHome);
  }

  @Test
  public void shouldLoadMavenHome() {
    File mavenHome = FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/config/FileSystemTest/maven"));
    Configuration config = Configuration.builder().setProperty("maven.home", mavenHome).build();
    FileSystem fileSystem = new FileSystem(config);

    assertThat(fileSystem.mavenHome()).isEqualTo(mavenHome);
  }

  @Test
  public void mavenHomeShouldExistIfDefined() {
    thrown.expect(IllegalArgumentException.class);
    Configuration config = Configuration.builder().setProperty("maven.home", "/invalid/path").build();
    new FileSystem(config);
  }

  @Test
  public void javaHomeShouldExistIfDefined() {
    thrown.expect(IllegalArgumentException.class);
    Configuration config = Configuration.builder().setProperty("java.home", "/invalid/path").build();
    new FileSystem(config);
  }

  @Test
  public void shouldLoadMavenLocalRepository() {
    File localRepository = FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/config/FileSystemTest/maven/repository"));
    Configuration config = Configuration.builder().setProperty("maven.localRepository", localRepository).build();
    FileSystem fileSystem = new FileSystem(config);

    assertThat(fileSystem.mavenLocalRepository()).isEqualTo(localRepository);
  }

  @Test
  public void mavenLocalRepositoryShouldExistIfDefined() {
    thrown.expect(IllegalArgumentException.class);
    Configuration config = Configuration.builder().setProperty("maven.localRepository", "/invalid/path").build();
    new FileSystem(config);
  }

  @Test
  public void shouldOverrideMavenRemoteRepository() {
    String url = "https://192.168.0.70/nexus";
    Configuration config = Configuration.builder()
        .setProperty("maven.nexusUrl", url)
        .setProperty("maven.nexusRepository", "ss-repo").build();
    FileSystem fileSystem = new FileSystem(config);

    assertThat(fileSystem.mavenNexusUrl().toString()).isEqualTo(url);
  }
}
