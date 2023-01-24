/*
 * Orchestrator
 * Copyright (C) 2011-2023 SonarSource SA
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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GradleBuildTest {

  @Test
  public void create() {
    Location projectDir = FileLocation.of(getClass().getResource("/com/sonar/orchestrator/build/GradleBuildTest"));
    GradleBuild build = GradleBuild.create(projectDir).addTask("build");

    assertThat(build.getTasks()).containsExactly("build");
    assertThat(build.getProjectDirectory()).isEqualTo(projectDir);
    assertThat(build.getTimeoutSeconds()).isPositive();
    assertThat(build.getProperties()).isEmpty();
  }

  @Test
  public void create_from_file() {
    File projectDir = new File("some_nice_project");
    GradleBuild build = GradleBuild.create(projectDir);

    assertThat(build.getProjectDirectory()).isEqualTo(FileLocation.of(projectDir));
  }

  @Test
  public void add_tasks() {
    GradleBuild build = GradleBuild.create();
    assertThat(build.getTasks()).isEmpty();

    build.addTask("clean").addTask("build");
    assertThat(build.getTasks()).containsExactly("clean", "build");
  }

  @Test
  public void set_tasks() {
    GradleBuild build = GradleBuild.create().setTasks("clean", "build", "sonarqube");
    assertThat(build.getTasks()).containsExactly("clean", "build", "sonarqube");
  }

  @Test
  public void add_sonar_task() {
    GradleBuild build = GradleBuild.create().setTasks("clean", "build").addSonarTask();
    assertThat(build.getTasks()).containsExactly("clean", "build", "sonarqube");
  }
}
