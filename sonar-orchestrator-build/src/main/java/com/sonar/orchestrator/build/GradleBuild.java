/*
 * Orchestrator Build
 * Copyright (C) 2011-2025 SonarSource Sàrl
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

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.sonar.orchestrator.util.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public final class GradleBuild extends Build<GradleBuild> {

  private Location projectDirectory;
  private List<String> tasks = new ArrayList<>();

  @Override
  BuildResult execute(Configuration config, Locators locators, Map<String, String> adjustedProperties) {
    return new GradleBuildExecutor().execute(this, config, locators, adjustedProperties);
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
    this.tasks = new ArrayList<>(tasks);
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
  
  public static GradleBuild create(File projectDirectory) {
    return create(FileLocation.of(projectDirectory));
  }
}
