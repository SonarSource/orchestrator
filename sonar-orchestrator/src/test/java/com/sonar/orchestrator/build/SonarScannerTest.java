/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.version.Version;
import java.io.File;
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
      .setBinaries("target/classes")
      .setDebugLogs(true)
      .setShowErrors(false)
      .setTask("task")
      .addArgument("-X")
      .addArguments("--help")
      .setLibraries("lib/guava.jar")
      .setProperty("foo", "bar")
      .setProperty("no_value", null)
      .setProperties(ImmutableMap.of("one", "1"))
      .setRunnerVersion("1.4")
      .setLanguage("java")
      .setSourceEncoding("UTF-8")
      .setUseOldSonarRunnerScript(true)
      .setProfile("my profile");

    assertThat(build.getProjectDir()).isEqualTo(new File("."));
    assertThat(build.runnerVersion()).isEqualTo(Version.create("1.4"));
    assertThat(build.getTask()).isEqualTo("task");
    assertThat(build.arguments()).containsExactly("-X", "--help");
    assertThat(build.getProperties().get("sonar.projectKey")).isEqualTo("SAMPLE");
    assertThat(build.getProperties().get("sonar.projectName")).isEqualTo("Sample");
    assertThat(build.getProperties().get("sonar.projectVersion")).isEqualTo("1.2.3");
    assertThat(build.getProperties().get("sonar.language")).isEqualTo("java");
    assertThat(build.getProperties().get("sonar.sources")).isEqualTo("src/main/java,src/java");
    assertThat(build.getProperties().get("sonar.tests")).isEqualTo("src/test/java,test/java");
    assertThat(build.getProperties().get("sonar.binaries")).isEqualTo("target/classes");
    assertThat(build.getProperties().get("sonar.libraries")).isEqualTo("lib/guava.jar");
    assertThat(build.getProperties().get("foo")).isEqualTo("bar");
    assertThat(build.getProperties().get("one")).isEqualTo("1");
    assertThat(build.getProperties().get("no_value")).isNull();
    assertThat(build.getProperties().get("sonar.sourceEncoding")).isEqualTo("UTF-8");
    assertThat(build.getProperties().get("sonar.profile")).isEqualTo("my profile");
    assertThat(build.isDebugLogs()).isTrue();
    assertThat(build.isShowErrors()).isFalse();
    assertThat(build.isUseOldSonarRunnerScript()).isTrue();

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
    assertThat(build.runnerVersion()).isEqualTo(Version.create(SonarScanner.DEFAULT_SCANNER_VERSION));
    assertThat(build.getProperties().get("sonar.sourceEncoding")).isEqualTo(SonarScanner.DEFAULT_SOURCE_ENCODING);
    // check assigned values
    assertThat(build.getProperties().get("sonar.projectKey")).isEqualTo("SAMPLE");
    assertThat(build.getProperties().get("sonar.projectName")).isEqualTo("Sample");
    assertThat(build.getProperties().get("sonar.projectVersion")).isNull();
    assertThat(build.isDebugLogs()).isTrue();
    assertThat(build.isShowErrors()).isTrue();
    // check reset default value for source encoding
    build.setSourceEncoding("CP-1252");
    assertThat(build.getProperties().get("sonar.sourceEncoding")).isEqualTo("CP-1252");
    build.setSourceEncoding(null);
    assertThat(build.getProperties().get("sonar.sourceEncoding")).isEqualTo(SonarScanner.DEFAULT_SOURCE_ENCODING);
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
    assertThat(build.runnerVersion().toString()).isNotEmpty();
    assertThat(build.getProperties().get("sonar.projectKey")).isNull();
    assertThat(build.getProperties().get("sonar.projectName")).isNull();
    assertThat(build.getProperties().get("sonar.projectVersion")).isNull();
  }

  @Test
  public void runner_version_must_be_set() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("version must be set");

    SonarScanner.create().setRunnerVersion("");
  }

  @Test
  public void environment_variables() {
    SonarScanner build = SonarScanner.create();

    // defaults
    assertThat(build.getEffectiveEnvironmentVariables())
      .containsOnly(MapEntry.entry("SONAR_RUNNER_OPTS", "-Djava.awt.headless=true"));

    build.setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx128m");
    assertThat(build.getEffectiveEnvironmentVariables())
      .containsOnly(MapEntry.entry("SONAR_RUNNER_OPTS", "-Djava.awt.headless=true -Xmx128m"));
  }
}
