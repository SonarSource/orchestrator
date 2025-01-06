/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
import static org.mockito.ArgumentMatchers.any;
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

  private final Locators locators = mock(Locators.class);
  private final SonarScannerInstaller installer = spy(new SonarScannerInstaller(locators));

  @Test
  public void unsupported_version() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Unsupported sonar-scanner version: 1.0");
    installer.install(Version.create("1.0"), temp.newFolder());
  }

  @Test
  public void install_embedded_version() throws Exception {
    File toDir = temp.newFolder();
    File script = installer.install(Version.create(SonarScanner.DEFAULT_SCANNER_VERSION), toDir);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("sonar-scanner");
    assertThat(script.getParentFile()).hasName("bin");
    assertThat(script.getParentFile().getParentFile()).hasName("sonar-scanner-" + SonarScanner.DEFAULT_SCANNER_VERSION);

    verify(locators, never()).locate(any(MavenLocation.class));
  }

  @Test
  public void install_platform_specific_flavor() throws Exception {
    File toDir = temp.newFolder();

    String classifier = "linux";
    String versionString = "2.9";
    Version version = Version.create(versionString);

    File script = installer.install(version, classifier, toDir);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("sonar-scanner");
    assertThat(script.getParentFile()).hasName("bin");
    assertThat(script.getParentFile().getParentFile()).hasName("sonar-scanner-" + versionString + "-linux");

    Path javaPath = script.toPath().getParent().resolve("../lib/jre/bin/java");
    assertThat(javaPath).isRegularFile();

    verify(locators, never()).locate(any(MavenLocation.class));
  }

  @Test
  public void find_version_available_in_maven_repositories() throws Exception {
    File toDir = temp.newFolder();
    when(locators.locate(SonarScannerInstaller.mavenLocation(Version.create("6.0-SNAPSHOT")))).thenReturn(
      new File(getClass().getResource("/com/sonar/orchestrator/build/SonarScannerInstallerTest/sonar-scanner-6.0-SNAPSHOT.zip").toURI()));

    // we're sure that SNAPSHOT versions are not embedded in sonar-scanner
    File script = installer.install(Version.create("6.0-SNAPSHOT"), toDir);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("sonar-scanner");
    assertThat(script.getParentFile()).hasName("bin");
    assertThat(script.getParentFile().getParentFile()).hasName("sonar-scanner-6.0-SNAPSHOT");
  }

  @Test
  public void do_not_install_twice() throws Exception {
    File toDir = temp.newFolder();

    installer.install(Version.create(SonarScanner.DEFAULT_SCANNER_VERSION), toDir);
    installer.install(Version.create(SonarScanner.DEFAULT_SCANNER_VERSION), toDir);

    verify(installer, times(1)).doInstall(Version.create(SonarScanner.DEFAULT_SCANNER_VERSION), null, toDir);
  }

  @Test
  public void should_not_keep_cache_of_snapshot_versions() throws Exception {
    File toDir = temp.newFolder();
    when(locators.locate(SonarScannerInstaller.mavenLocation(Version.create("6.0-SNAPSHOT")))).thenReturn(
      new File(getClass().getResource("/com/sonar/orchestrator/build/SonarScannerInstallerTest/sonar-scanner-6.0-SNAPSHOT.zip").toURI()));

    installer.install(Version.create("6.0-SNAPSHOT"), toDir);
    installer.install(Version.create("6.0-SNAPSHOT"), toDir);

    verify(installer, times(2)).doInstall(Version.create("6.0-SNAPSHOT"), null, toDir);
  }

  @Test
  public void maven_location() {
    MavenLocation location = SonarScannerInstaller.mavenLocation(Version.create("2.5"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.getVersion()).isEqualTo("2.5");
    assertThat(location.getGroupId()).isEqualTo("org.sonarsource.scanner.cli");
    assertThat(location.getArtifactId()).isEqualTo("sonar-scanner-cli");
  }
}
