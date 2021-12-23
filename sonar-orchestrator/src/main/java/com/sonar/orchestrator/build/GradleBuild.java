/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.Location;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;

public final class GradleBuild extends Build<GradleBuild> {

  private Location projectDirectory;
  private List<String> tasks = new ArrayList<>();

  @Override
  BuildResult execute(Configuration config, Map<String, String> adjustedProperties) {
    return new GradleBuildExecutor().execute(this, config, adjustedProperties);
  }

  public GradleBuild setProjectDirectory(Location projectDirectory) {
    this.projectDirectory = projectDirectory;
    return this;
  }

  public Location getProjectDirectory() {
    return this.projectDirectory;
  }

  public GradleBuild addTask(String task) {
    checkArgument(!isEmpty(task), "Gradle task must be set");
    this.tasks.add(task);
    return this;
  }

  public GradleBuild addSonarTask() {
    this.tasks.add("sonarqube");
    return this;
  }

  public GradleBuild setTasks(String... tasks) {
    return setTasks(Arrays.asList(tasks));
  }

  public GradleBuild setTasks(List<String> tasks) {
    checkArgument(!tasks.isEmpty(), "At least one task must be set");
    this.tasks = tasks;
    return this;
  }

  public List<String> getTasks() {
    return this.tasks;
  }

  public static GradleBuild create() {
    return new GradleBuild();
  }

  public static GradleBuild create(Location projectDirectory) {
    return create().setProjectDirectory(projectDirectory);
  }

}
