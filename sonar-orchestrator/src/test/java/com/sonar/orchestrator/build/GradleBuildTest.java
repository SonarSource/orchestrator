/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
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
    GradleBuild build = GradleBuild.create().addSonarTask();
    assertThat(build.getTasks()).containsExactly("sonarqube");
  }
}
