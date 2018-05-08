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
import com.sonar.orchestrator.version.Version;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

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
      .setUseOldRunnerScript(true)
      .setScannerLocation(scannerLocation);

    assertThat(build.getProjectDir()).isEqualTo(new File("."));
    assertThat(build.scannerVersion()).isEqualTo(Version.create("2.2"));
    assertThat(build.getProjectKey()).isEqualTo("SAMPLE");
    assertThat(build.getProjectName()).isEqualTo("Sample");
    assertThat(build.getProjectVersion()).isEqualTo("1.2.3");
    assertThat(build.isDebugLogs()).isTrue();
    assertThat(build.isUseOldRunnerScript()).isTrue();
    assertThat(build.getLocation()).isEqualTo(scannerLocation);
  }

  @Test
  public void test_create_with_props() throws Exception {
    File projectDir = temp.newFolder();
    ScannerForMSBuild build = ScannerForMSBuild.create(projectDir, "sonar.foo", "bar");

    assertThat(build.getProjectDir()).isEqualTo(projectDir);
    assertThat(build.getProperty("sonar.foo")).isEqualTo("bar");
    assertThat(build.isUseOldRunnerScript()).isFalse();
  }

  @Test
  public void test_use_old_script() {
    ScannerForMSBuild build = ScannerForMSBuild.create();
    assertThat(build.isUseOldRunnerScript()).isFalse();

    build.setScannerVersion("2.2");
    assertThat(build.isUseOldRunnerScript()).isFalse();

    build.setScannerVersion("2.1");
    assertThat(build.isUseOldRunnerScript()).isTrue();

    build.setScannerVersion("2.2");
    build.setUseOldRunnerScript(true);
    assertThat(build.isUseOldRunnerScript()).isTrue();

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
    thrown.expectMessage("Version of ScannerForMSBuild should be higher than or equals to 4.1.0.1148 to be able to use .Net Core.");

    build.check();
  }
}
