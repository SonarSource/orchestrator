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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NOTE FOR IDE
 * Check that zip files are correctly loaded in IDE classpath during execution of tests
 */
public class SonarScannerInstallerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Locators locators = mock(Locators.class);
  private SonarScannerInstaller installer = spy(new SonarScannerInstaller(locators));

  @Test
  public void unsupported_version() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Unsupported sonar-scanner version: 1.0");
    installer.install(Version.create("1.0"), temp.newFolder(), true);
  }

  @Test
  public void install_embedded_version() throws Exception {
    File toDir = temp.newFolder();
    File script = installer.install(Version.create(SonarRunner.DEFAULT_SCANNER_VERSION), toDir, false);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("sonar-scanner");
    assertThat(script.getParentFile().getName()).isEqualTo("bin");
    assertThat(script.getParentFile().getParentFile().getName()).isEqualTo("sonar-scanner-" + SonarRunner.DEFAULT_SCANNER_VERSION);

    verify(locators, never()).locate(any(MavenLocation.class));
  }

  @Test
  public void install_self_contained() throws Exception {
    File toDir = temp.newFolder();

    String classifier = "linux";
    String versionString = "2.9";
    Version version = Version.create(versionString);

    File script = installer.install(version, classifier, toDir, false);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("sonar-scanner");
    assertThat(script.getParentFile().getName()).isEqualTo("bin");
    assertThat(script.getParentFile().getParentFile().getName()).isEqualTo("sonar-scanner-" + versionString + "-linux");

    Path javaPath = script.toPath().getParent().resolve("../lib/jre/bin/java");
    assertThat(javaPath).isRegularFile();

    verify(locators, never()).locate(any(MavenLocation.class));
  }

  @Test
  public void find_version_available_in_maven_repositories() throws Exception {
    File toDir = temp.newFolder();
    when(locators.locate(SonarScannerInstaller.mavenLocation(Version.create("1.4-SNAPSHOT")))).thenReturn(
      new File(getClass().getResource("/com/sonar/orchestrator/build/SonarRunnerInstallerTest/sonar-runner-1.4-SNAPSHOT.zip").toURI()));

    // we're sure that SNAPSHOT versions are not embedded in sonar-runner
    File script = installer.install(Version.create("1.4-SNAPSHOT"), toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("sonar-runner");
    assertThat(script.getParentFile().getName()).isEqualTo("bin");
    assertThat(script.getParentFile().getParentFile().getName()).isEqualTo("sonar-runner-1.4-SNAPSHOT");
  }

  @Test
  public void new_sonar_scanner_script() throws Exception {
    File toDir = temp.newFolder();
    when(locators.locate(SonarScannerInstaller.mavenLocation(Version.create("2.6-SNAPSHOT")))).thenReturn(
      new File(getClass().getResource("/com/sonar/orchestrator/build/SonarRunnerInstallerTest/sonar-scanner-2.6-SNAPSHOT.zip").toURI()));

    File script = installer.install(Version.create("2.6-SNAPSHOT"), toDir, false);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("sonar-scanner");
    assertThat(script.getParentFile().getName()).isEqualTo("bin");
    assertThat(script.getParentFile().getParentFile().getName()).isEqualTo("sonar-scanner-2.6-SNAPSHOT");
  }

  @Test
  public void do_not_install_twice() throws Exception {
    File toDir = temp.newFolder();

    installer.install(Version.create(SonarRunner.DEFAULT_SCANNER_VERSION), toDir, false);
    installer.install(Version.create(SonarRunner.DEFAULT_SCANNER_VERSION), toDir, false);

    verify(installer, times(1)).doInstall(Version.create(SonarRunner.DEFAULT_SCANNER_VERSION), null, toDir);
  }

  @Test
  public void should_not_keep_cache_of_snapshot_versions() throws Exception {
    File toDir = temp.newFolder();
    when(locators.locate(SonarScannerInstaller.mavenLocation(Version.create("1.4-SNAPSHOT")))).thenReturn(
      new File(getClass().getResource("/com/sonar/orchestrator/build/SonarRunnerInstallerTest/sonar-runner-1.4-SNAPSHOT.zip").toURI()));

    installer.install(Version.create("1.4-SNAPSHOT"), toDir, true);
    installer.install(Version.create("1.4-SNAPSHOT"), toDir, true);

    verify(installer, times(2)).doInstall(Version.create("1.4-SNAPSHOT"), null, toDir);
  }

  @Test
  public void maven_location_before_2_1() {
    MavenLocation location = SonarScannerInstaller.mavenLocation(Version.create("1.2.3"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.getVersion()).isEqualTo("1.2.3");
    assertThat(location.getGroupId()).isEqualTo("org.codehaus.sonar-plugins");
    assertThat(location.getArtifactId()).isEqualTo("sonar-runner");
  }

  @Test
  public void maven_location() {
    MavenLocation location = SonarScannerInstaller.mavenLocation(Version.create("2.1-SNAPSHOT"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.getVersion()).isEqualTo("2.1-SNAPSHOT");
    assertThat(location.getGroupId()).isEqualTo("org.codehaus.sonar.runner");
    assertThat(location.getArtifactId()).isEqualTo("sonar-runner-dist");
  }

  @Test
  public void maven_location_of_2_5() {
    MavenLocation location = SonarScannerInstaller.mavenLocation(Version.create("2.5"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.getVersion()).isEqualTo("2.5");
    assertThat(location.getGroupId()).isEqualTo("org.sonarsource.scanner.cli");
    assertThat(location.getArtifactId()).isEqualTo("sonar-scanner-cli");
  }
}
