/*
 * Orchestrator Build
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

import com.sonar.orchestrator.build.version.Version;
import java.io.File;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarScannerTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_create() {
    SonarScanner build = SonarScanner.create()
      .setProjectDir(new File("."))
      .setProjectKey("SAMPLE")
      .setProjectName("Sample")
      .setProjectVersion("1.2.3")
      .setSourceDirs("src/main/java,src/java")
      .setTestDirs("src/test/java,test/java")
      .setDebugLogs(true)
      .setShowErrors(false)
      .addArgument("-X")
      .addArguments("--help")
      .setProperty("foo", "bar")
      .setProperty("no_value", null)
      .setProperties(Map.of("one", "1"))
      .setScannerVersion("1.4")
      .setSourceEncoding("UTF-8")
      .setProfile("my profile")
      .setClassifier("linux-x64");

    assertThat(build.getProjectDir()).isEqualTo(new File("."));
    assertThat(build.scannerVersion()).isEqualTo(Version.create("1.4"));
    assertThat(build.arguments()).containsExactly("-X", "--help");
    assertThat(build.getProperties()).containsEntry("sonar.projectKey", "SAMPLE");
    assertThat(build.getProperties()).containsEntry("sonar.projectName", "Sample");
    assertThat(build.getProperties()).containsEntry("sonar.projectVersion", "1.2.3");
    assertThat(build.getProperties()).containsEntry("sonar.sources", "src/main/java,src/java");
    assertThat(build.getProperties()).containsEntry("sonar.tests", "src/test/java,test/java");
    assertThat(build.getProperties()).containsEntry("foo", "bar");
    assertThat(build.getProperties()).containsEntry("one", "1");
    assertThat(build.getProperties()).doesNotContainKey("no_value");
    assertThat(build.getProperties()).containsEntry("sonar.profile", "my profile");
    assertThat(build.isDebugLogs()).isTrue();
    assertThat(build.isShowErrors()).isFalse();
    assertThat(build.classifier()).isEqualTo("linux-x64");

    build.setScannerVersion("2.5");
    assertThat(build.scannerVersion()).isEqualTo(Version.create("2.5"));
  }

  @Test
  public void test_enhanced_create() {
    SonarScanner build = SonarScanner.create(new File("."),
      "sonar.projectKey", "SAMPLE",
      "sonar.projectName", "Sample").setDebugLogs(true);

    assertThat(build.getProjectDir()).isEqualTo(new File("."));
    // check default values
    assertThat(build.scannerVersion()).isEqualTo(Version.create(SonarScanner.DEFAULT_SCANNER_VERSION));
    // check assigned values
    assertThat(build.getProperties()).containsEntry("sonar.projectKey", "SAMPLE");
    assertThat(build.getProperties()).containsEntry("sonar.projectName", "Sample");
    assertThat(build.getProperties()).doesNotContainKey("sonar.projectVersion");
    assertThat(build.isDebugLogs()).isTrue();
    assertThat(build.isShowErrors()).isTrue();

    build.setSourceEncoding("CP-1252");
    assertThat(build.getProperties()).containsEntry("sonar.sourceEncoding", "CP-1252");
  }


  @Test
  public void test_classifier_autodetection_before_6_1() {
    SonarScanner build = SonarScanner.create(new File("."))
      .setScannerVersion("6.0");
    assertThat(build.classifier()).isNull();

    build.useNative();
    assertThat(build.classifier()).isNotEmpty().doesNotContain("-");
  }

  @Test
  public void test_classifier_autodetection_after_6_1() {
    SonarScanner build = SonarScanner.create(new File("."))
      .setScannerVersion("6.1");
    assertThat(build.classifier()).isNull();

    build.useNative();
    assertThat(build.classifier()).isNotEmpty().contains("-");
  }

  @Test
  public void test_os_detection() {
    assertThat(SonarScanner.determineOs(() -> true, () -> false, () -> false)).isEqualTo("linux");
    assertThat(SonarScanner.determineOs(() -> false, () -> true, () -> false)).isEqualTo("windows");
    assertThat(SonarScanner.determineOs(() -> false, () -> false, () -> true)).isEqualTo("macosx");
  }

  @Test
  public void test_arch_detection() {
    assertThat(SonarScanner.determineArchitecture(() -> true, () -> false, () -> false, () -> "aarch64")).isEqualTo("aarch64");
    assertThat(SonarScanner.determineArchitecture(() -> true, () -> false, () -> false, () -> "notaarch64")).isEqualTo("x64");
    assertThat(SonarScanner.determineArchitecture(() -> false, () -> true, () -> false, () -> "ignored")).isEqualTo("x64");
    assertThat(SonarScanner.determineArchitecture(() -> false, () -> false, () -> true, () -> "aarch64")).isEqualTo("aarch64");
    assertThat(SonarScanner.determineArchitecture(() -> false, () -> false, () -> true, () -> "notaarch64")).isEqualTo("x64");
  }

  @Test
  public void fails_if_project_dir_not_set() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Project directory must be set");

    SonarScanner.create().check();
  }

  @Test
  public void fails_if_project_dir_not_found() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Project directory must exist");

    SonarScanner.create().setProjectDir(new File("does/not/exist"));
  }

  @Test
  public void project_dir_is_enough_when_it_contains_properties_file() {
    SonarScanner build = SonarScanner.create()
      .setProjectDir(new File("."));

    assertThat(build.getProjectDir()).isEqualTo(new File("."));
    assertThat(build.getProperties().get("sonar.projectKey")).isNull();
    assertThat(build.getProperties().get("sonar.projectName")).isNull();
    assertThat(build.getProperties().get("sonar.projectVersion")).isNull();
  }

  @Test
  public void scanner_version_must_be_set() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("version must be set");

    SonarScanner.create().setScannerVersion("");
  }

  @Test
  public void environment_variables() {
    SonarScanner build = SonarScanner.create();

    build.setEnvironmentVariable("SOME_ENV", "aValue");
    assertThat(build.getEffectiveEnvironmentVariables())
      .containsOnly(MapEntry.entry("SOME_ENV", "aValue"));
  }
}
