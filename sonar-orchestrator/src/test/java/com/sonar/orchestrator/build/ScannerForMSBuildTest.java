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

import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.GitHub;
import com.sonar.orchestrator.version.Version;
import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerForMSBuildTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_create() throws Exception {
    FileLocation scannerLocation = FileLocation.of(temp.newFile());
    ScannerForMSBuild build = ScannerForMSBuild.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setProjectName("Sample")
      .setProjectVersion("1.2.3")
      .setDebugLogs(true)
      .setScannerVersion("2.2")
      .setScannerLocation(scannerLocation);

    assertThat(build.getProjectDir()).isEqualTo(new File("."));
    assertThat(build.scannerVersion()).isEqualTo(Version.create("2.2"));
    assertThat(build.getProjectKey()).isEqualTo("SAMPLE");
    assertThat(build.getProjectName()).isEqualTo("Sample");
    assertThat(build.getProjectVersion()).isEqualTo("1.2.3");
    assertThat(build.isDebugLogs()).isTrue();
    assertThat(build.getLocation()).isEqualTo(scannerLocation);
  }

  @Test
  public void test_create_with_props() throws Exception {
    File projectDir = temp.newFolder();
    ScannerForMSBuild build = ScannerForMSBuild.create(projectDir, "sonar.foo", "bar");

    assertThat(build.getProjectDir()).isEqualTo(projectDir);
    assertThat(build.getProperty("sonar.foo")).isEqualTo("bar");
  }

  @Test
  public void test_dotnet_core_off_by_default() {
    ScannerForMSBuild build = ScannerForMSBuild.create();
    assertThat(build.isUsingDotNetCore()).isFalse();

    build.setUseDotNetCore(true);
    assertThat(build.isUsingDotNetCore()).isTrue();
  }

  @Test
  public void test_dotnet_core_checks_version_if_set_before() {
    ScannerForMSBuild build = ScannerForMSBuild.create();
    build.setScannerVersion("4.2");
    assertThat(build.isUsingDotNetCore()).isFalse();

    build.setUseDotNetCore(true);
    assertThat(build.isUsingDotNetCore()).isTrue();

    build = ScannerForMSBuild.create();
    assertThat(build.isUsingDotNetCore()).isFalse();
    build.setDotNetCoreExecutable(new File("."));
    assertThat(build.isUsingDotNetCore()).isTrue();

    build = ScannerForMSBuild.create();
    build.setScannerVersion("2.2");
    build.setUseDotNetCore(true);
    build.setProjectDir(new File("."));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Version of ScannerForMSBuild should be greater than or equals to 4.1 to be able to use .Net Core.");

    build.check();
  }

  @Test
  public void test_setScannerVersion_with_specific_version() {
    String version = "6.0.0.0";
    ScannerForMSBuild build = ScannerForMSBuild.create().setScannerVersion(version);
    assertThat(build.scannerVersion()).isEqualTo(Version.create(version));
  }

  @Test
  public void test_setScannerVersion_with_latest_version() {
    String version = "6.0.0.0";
    GitHub gitHub = mock(GitHub.class);
    when(gitHub.getLatestScannerReleaseVersion()).thenReturn(version);
    ScannerForMSBuild sut = new ScannerForMSBuild(gitHub);
    ScannerForMSBuild build = sut.setScannerVersion(ScannerForMSBuild.LATEST_RELEASE);
    assertThat(build.scannerVersion()).isEqualTo(Version.create(version));
  }

  @Test
  public void test_setScannerVersion_with_wrong_version() {
    ScannerForMSBuild build = ScannerForMSBuild.create();
    assertThatThrownBy(() -> build.setScannerVersion("wrong version")).isInstanceOf(Version.VersionParsingException.class);
  }
}
