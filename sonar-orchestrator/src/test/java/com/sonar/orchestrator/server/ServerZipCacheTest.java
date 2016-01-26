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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerZipCacheTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  FileSystem fs;
  ServerZipCache underTest;
  File installsDir;
  File workspaceDir;

  @Before
  public void setUp() throws IOException {
    installsDir = temp.newFolder();
    workspaceDir = temp.newFolder();
    Configuration config = Configuration.builder()
      .setProperty("orchestrator.sonarInstallsDir", installsDir.getAbsolutePath())
      .setProperty("orchestrator.workspaceDir", workspaceDir.getAbsolutePath())
      .build();
    fs = new FileSystem(config);
    underTest = new ServerZipCache(fs);
    underTest.clear();
  }

  @Test
  public void get_not_cached() {
    assertThat(underTest.get(Version.create("5.2"))).isNull();
    assertThat(underTest.get(Version.create("5.2-SNAPSHOT"))).isNull();
  }

  @Test
  public void add_release_to_user_cache() throws IOException {
    File zip = temp.newFile("sonarqube-5.2.zip");
    Version version = Version.create("5.2");
    File cached = underTest.moveToCache(version, zip);
    assertThat(cached.getName()).isEqualTo("sonarqube-5.2.zip");
    assertThat(cached.getParentFile()).isEqualTo(installsDir);
    // file is moved to cache, not copied
    assertThat(zip).doesNotExist();

    assertThat(underTest.get(version)).isEqualTo(cached);
  }

  @Test
  public void add_snapshot_to_workspace_cache() throws IOException {
    File zip = temp.newFile("sonar-application-5.2-SNAPSHOT.zip");
    Version version = Version.create("5.2-SNAPSHOT");
    File cached = underTest.moveToCache(version, zip);
    assertThat(cached.getName()).isEqualTo("sonar-application-5.2-SNAPSHOT.zip");
    assertThat(cached.getParentFile()).isEqualTo(workspaceDir);

    assertThat(underTest.get(version)).isEqualTo(cached);
  }

  @Test
  public void throw_ISE_if_fail_to_move_zip_to_cache() throws IOException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to move SonarQube zip");

    File zip = temp.newFile("sonarqube-5.2.zip");
    Version version = Version.create("5.2");
    // force move to fail
    zip.delete();
    underTest.moveToCache(version, zip);
  }
}
