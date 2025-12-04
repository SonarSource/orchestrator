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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.build.dotnet.scanner.PackageDetails;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

  private final Locators locators = mock(Locators.class);
  private final ScannerForMSBuildInstaller installer = new ScannerForMSBuildInstaller(locators);

  @Test
  public void install_embedded_version() throws Exception {
    File toDir = temp.newFolder();
    File script = installer.install(Version.create(ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION), null, toDir, false);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.exe");
    assertThat(script.getParentFile()).hasName("sonar-scanner-msbuild-" + ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION + "-" + "net46");

    verify(locators, never()).locate(any(MavenLocation.class));
  }

  @Test
  public void install_zip() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstaller.class.getResource("/com/sonar/orchestrator/build/sonar-scanner-msbuild-" + ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION + "-net46.zip");
    FileLocation zipLocation = FileLocation.of(new File(zip.toURI()));
    when(locators.locate(zipLocation)).thenReturn(new File(zip.toURI()));
    File script = installer.install(null, zipLocation, toDir, false);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.exe");
    assertThat(script.getParentFile()).hasName("sonar-scanner");
  }

  @Test
  public void install_zip_after_dot_net_core_support() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstallerTest.class.getResource("/com/sonar/orchestrator/build/ScannerForMSBuildInstallerTest/sonar-scanner-msbuild-4.2.0.1214-net46.zip");
    FileLocation zipLocation = FileLocation.of(new File(zip.toURI()));
    when(locators.locate(zipLocation)).thenReturn(new File(zip.toURI()));
    File script = installer.install(null, zipLocation, toDir, false);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.exe");
    assertThat(script.getParentFile()).hasName("sonar-scanner");
  }

  @Test
  public void install_zip_after_dot_net_core_support_with_dot_net_core() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstallerTest.class.getResource("/com/sonar/orchestrator/build/ScannerForMSBuildInstallerTest/sonar-scanner-msbuild-4.2.0.1214-netcoreapp2.0.zip");
    FileLocation zipLocation = FileLocation.of(new File(zip.toURI()));
    when(locators.locate(zipLocation)).thenReturn(new File(zip.toURI()));
    File script = installer.install(null, zipLocation, toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.dll");
    assertThat(script.getParentFile()).hasName("sonar-scanner");
  }

  @Test
  public void install_zip_after_dot_net_core_support_by_providing_version() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstallerTest.class.getResource("/com/sonar/orchestrator/build/ScannerForMSBuildInstallerTest/sonar-scanner-msbuild-4.2.0.1214-net46.zip");
    when(locators.locate(any())).thenReturn(new File(zip.toURI()));
    File script = installer.install(Version.create("4.2.0.1214"), null, toDir, false);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.exe");
    assertThat(script.getParentFile()).hasName("sonar-scanner-msbuild-4.2.0.1214-net46");
  }

  @Test
  public void install_zip_after_dot_net_core_support_by_providing_version_dot_net_core() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstallerTest.class.getResource("/com/sonar/orchestrator/build/ScannerForMSBuildInstallerTest/sonar-scanner-msbuild-4.2.0.1214-netcoreapp2.0.zip");
    when(locators.locate(any())).thenReturn(new File(zip.toURI()));
    File script = installer.install(Version.create("4.2.0.1214"), null, toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.dll");
    assertThat(script.getParentFile()).hasName("sonar-scanner-msbuild-4.2.0.1214-netcoreapp2.0");
  }

  @Test
  public void install_zip_after_package_renaming_dotnet_core() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstallerTest.class.getResource("/com/sonar/orchestrator/build/ScannerForMSBuildInstallerTest/sonar-scanner-6.0.0.81631-net.zip");
    when(locators.locate(any())).thenReturn(new File(zip.toURI()));
    File script = installer.install(Version.create("6.0.0.81631"), null, toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.dll");
    assertThat(script.getParentFile()).hasName("sonar-scanner-6.0.0.81631-net");
  }

  @Test
  public void install_scanner_cli_not_bundled_version() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstallerTest.class.getResource("/com/sonar/orchestrator/build/ScannerForMSBuildInstallerTest/sonar-scanner-11.0.0.126294-net.zip");
    when(locators.locate(any())).thenReturn(new File(zip.toURI()));
    File script = installer.install(Version.create("11.0.0.126294"), null, toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.dll");
    assertThat(script.getParentFile()).hasName("sonar-scanner-11.0.0.126294-net");
  }

  @Test
  public void install_zip_after_package_renaming_dotnet_framework() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstallerTest.class.getResource("/com/sonar/orchestrator/build/ScannerForMSBuildInstallerTest/sonar-scanner-6.0.0.81631-net-framework.zip");
    when(locators.locate(any())).thenReturn(new File(zip.toURI()));
    File script = installer.install(Version.create("6.0.0.81631"), null, toDir, false);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.exe");
    assertThat(script.getParentFile()).hasName("sonar-scanner-6.0.0.81631-net-framework");
  }

  @Test
  public void install_without_version_nor_location_should_install_default() throws IOException {
    File toDir = temp.newFolder();
    File script = installer.install(null, null, toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("SonarScanner.MSBuild.dll");
    assertThat(script.getParentFile()).hasName("sonar-scanner-msbuild-" + ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION + "-netcoreapp2.0");
  }

  @Test
  public void do_not_install_twice_with_version() throws Exception {
    File toDir = temp.newFolder();
    File script = installer.install(Version.create(ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION), null, toDir, false);
    File txt = new File(script.getParentFile(), "text.txt");
    txt.createNewFile();
    assertThat(txt).exists();

    installer.install(Version.create(ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION), null, toDir, false);
    assertThat(txt).exists();
  }

  @Test
  public void do_install_twice_with_location() throws Exception {
    File toDir = temp.newFolder();
    URL zip = ScannerForMSBuildInstaller.class.getResource("/com/sonar/orchestrator/build/sonar-scanner-msbuild-" + ScannerForMSBuildInstaller.DEFAULT_SCANNER_VERSION + "-net46.zip");
    FileLocation zipLocation = FileLocation.of(new File(zip.toURI()));
    when(locators.locate(zipLocation)).thenReturn(new File(zip.toURI()));

    File script = installer.install(null, zipLocation, toDir, false);
    File txt = new File(script.getParentFile(), "text.txt");
    txt.createNewFile();
    assertThat(txt).exists();

    installer.install(null, zipLocation, toDir, false);
    assertThat(txt).doesNotExist();
  }

  @Test
  public void fail_if_file_doesnt_exist() throws Exception {
    File toDir = temp.newFolder();

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("File doesn't exist");
    installer.install(null, FileLocation.of(new File("unknown")), toDir, false);
  }

  @Test
  public void maven_location() {
    MavenLocation location = ScannerForMSBuildInstaller.mavenLocation(Version.create("2.1-SNAPSHOT"), new PackageDetails("org.sonarsource.scanner.msbuild", "sonar-scanner-msbuild", "not used", "not used", "not used"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.getVersion()).isEqualTo("2.1-SNAPSHOT");
    assertThat(location.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(location.getArtifactId()).isEqualTo("sonar-scanner-msbuild");
    assertThat(location.getClassifier()).isEmpty();
  }

  @Test
  public void maven_location_after_dot_net_core_support() {
    MavenLocation location = ScannerForMSBuildInstaller.mavenLocation(Version.create("4.2"), new PackageDetails("org.sonarsource.scanner.msbuild", "sonar-scanner-msbuild", "net46", "not used", "not used"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.getVersion()).isEqualTo("4.2");
    assertThat(location.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(location.getArtifactId()).isEqualTo("sonar-scanner-msbuild");
    assertThat(location.getClassifier()).isEqualTo("net46");
  }

  @Test
  public void maven_location_after_dot_net_core_support_with_dot_net_core() {
    MavenLocation location = ScannerForMSBuildInstaller.mavenLocation(Version.create("4.2"), new PackageDetails("org.sonarsource.scanner.msbuild", "sonar-scanner-msbuild", "netcoreapp2.0", "not used", "not used"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.getVersion()).isEqualTo("4.2");
    assertThat(location.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(location.getArtifactId()).isEqualTo("sonar-scanner-msbuild");
    assertThat(location.getClassifier()).isEqualTo("netcoreapp2.0");
  }
}
