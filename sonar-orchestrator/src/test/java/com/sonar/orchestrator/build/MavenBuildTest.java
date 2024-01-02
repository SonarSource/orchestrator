/*
 * Orchestrator
 * Copyright (C) 2011-2024 SonarSource SA
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
import com.sonar.orchestrator.locator.Location;
import java.io.File;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenBuildTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_create() {
    Location pom = FileLocation.of(getClass().getResource("/com/sonar/orchestrator/build/MavenBuildTest/pom.xml"));
    MavenBuild build = MavenBuild.create(pom).addGoal("install");

    assertThat(build.getGoals()).containsExactly("install");
    assertThat(build.getPom()).isEqualTo(pom);
    assertThat(build.getTimeoutSeconds()).isGreaterThan(0);
    assertThat(build.getProperties().isEmpty()).isTrue();
    assertThat(build.isDebugLogs()).isFalse();

    File pomFile = new File("pom.xml");
    build = MavenBuild.create(pomFile);
    assertThat(((FileLocation) build.getPom()).getFile()).isEqualTo(pomFile);
  }

  @Test
  public void test_add_goals() {
    MavenBuild build = MavenBuild.create();
    assertThat(build.getGoals()).isEmpty();

    build.addGoal("clean install");
    assertThat(build.getGoals()).containsExactly("clean install");

    build.addGoal("sonar:sonar");
    assertThat(build.getGoals()).containsExactly("clean install", "sonar:sonar");
  }

  @Test
  public void should_fail_if_add_nullgoal() {
    thrown.expect(IllegalArgumentException.class);

    MavenBuild build = MavenBuild.create();
    build.addGoal(null);
  }

  @Test
  public void set_clean_package_sonar_goals() {
    MavenBuild build = MavenBuild.create().setCleanPackageSonarGoals();
    assertThat(build.getGoals()).containsExactly("clean package", "sonar:sonar");
  }

  @Test
  public void set_clean_sonar_goals() {
    MavenBuild build = MavenBuild.create().setCleanSonarGoals();
    assertThat(build.getGoals()).containsExactly("clean sonar:sonar");
  }

  @Test
  public void environment_variables() {
    MavenBuild build = MavenBuild.create().setCleanSonarGoals();

    // defaults
    assertThat(build.getEffectiveEnvironmentVariables())
      .hasSize(1)
      .contains(MapEntry.entry("MAVEN_OPTS", "-Djava.awt.headless=true"));

    build.setEnvironmentVariable("MAVEN_OPTS", "-Xmx128m");
    assertThat(build.getEffectiveEnvironmentVariables())
      .hasSize(1)
      .contains(MapEntry.entry("MAVEN_OPTS", "-Djava.awt.headless=true -Xmx128m"));
  }

  @Test
  public void test_getter_setter_executionDir_dynamicanalysis() {
    MavenBuild build = MavenBuild.create();
    build.setExecutionDir(new File("would_be_a_nice_place_to_execute"));
    build.setDynamicAnalysis(true);

    assertThat(build.getExecutionDir()).isEqualTo(new File("would_be_a_nice_place_to_execute"));
    assertThat(build.getProperty("sonar.dynamicAnalysis")).isEqualTo("true");
  }
}
