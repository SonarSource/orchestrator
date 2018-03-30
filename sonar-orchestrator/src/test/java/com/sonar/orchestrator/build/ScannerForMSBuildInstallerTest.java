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

import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.net.URL;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NOTE FOR IDE
 * Check that zip files are correctly loaded in IDE classpath during execution of tests
 */
public class ScannerForMSBuildInstallerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Locators locators = mock(Locators.class);
  private ScannerForMSBuildInstaller installer = new ScannerForMSBuildInstaller(locators);

  @Test
  public void install_embedded_version() throws Exception {
    File toDir = temp.newFolder();
    File script = installer.install(Version.create(ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION), null, toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("MSBuild.SonarQube.Runner.exe");
    assertThat(script.getParentFile().getName()).isEqualTo("sonar-scanner-msbuild-" + ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION);

    verify(locators, never()).locate(any(MavenLocation.class));
  }

  @Test
  public void install_zip() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstaller.class.getResource("/com/sonar/orchestrator/build/sonar-scanner-msbuild-" + ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION + ".zip");
    FileLocation zipLocation = FileLocation.of(new File(zip.toURI()));
    when(locators.locate(zipLocation)).thenReturn(new File(zip.toURI()));
    File script = installer.install(null, zipLocation, toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("MSBuild.SonarQube.Runner.exe");
    assertThat(script.getParentFile().getName()).isEqualTo("sonar-scanner-msbuild");
  }

  @Test
  public void do_not_install_twice_with_version() throws Exception {
    File toDir = temp.newFolder();
    File script = installer.install(Version.create(ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION), null, toDir, true);
    File txt = new File(script.getParentFile(), "text.txt");
    txt.createNewFile();
    assertThat(txt).exists();

    installer.install(Version.create(ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION), null, toDir, true);
    assertThat(txt).exists();
  }

  @Test
  public void do_install_twice_with_location() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstaller.class.getResource("/com/sonar/orchestrator/build/sonar-scanner-msbuild-" + ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION + ".zip");
    FileLocation zipLocation = FileLocation.of(new File(zip.toURI()));
    when(locators.locate(zipLocation)).thenReturn(new File(zip.toURI()));

    File script = installer.install(null, zipLocation, toDir, true);
    File txt = new File(script.getParentFile(), "text.txt");
    txt.createNewFile();
    assertThat(txt).exists();

    installer.install(null, zipLocation, toDir, true);
    assertThat(txt).doesNotExist();
  }

  @Test
  public void fail_if_file_doesnt_exist() throws Exception {
    File toDir = temp.newFolder();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("File doesn't exist");
    installer.install(null, FileLocation.of(new File("unknown")), toDir, true);
  }

  @Test
  public void maven_location() {
    MavenLocation location = ScannerForMSBuildInstaller.mavenLocation(Version.create("2.1-SNAPSHOT"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.getVersion()).isEqualTo("2.1-SNAPSHOT");
    assertThat(location.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(location.getArtifactId()).isEqualTo("sonar-scanner-msbuild");
  }

  @Test
  public void corrupted_zip() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to unzip Scanner for MSBuild");
    installer.install(Version.create("corrupted"), null, temp.newFolder(), true);
  }
}
