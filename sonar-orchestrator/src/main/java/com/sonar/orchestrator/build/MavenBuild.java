/*
 * Orchestrator
 * Copyright (C) 2011-2020 SonarSource SA
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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;

public final class MavenBuild extends Build<MavenBuild> {

  private static final Map<String, String> ENV_VARIABLES;
  static {
    Map<String, String> map = new HashMap<>();
    map.put("MAVEN_OPTS", "-Djava.awt.headless=true");
    ENV_VARIABLES = Collections.unmodifiableMap(map);
  }

  private Location pom;
  private File executionDir;
  private List<String> goals = new ArrayList<>();
  private boolean debugLogs = false;

  private MavenBuild() {
  }

  @Override
  protected Map<String, String> doGetEnvironmentVariablePrefixes() {
    return ENV_VARIABLES;
  }

  public Location getPom() {
    return pom;
  }

  public MavenBuild setPom(Location pom) {
    this.pom = pom;
    return this;
  }

  public MavenBuild setPom(File pom) {
    return setPom(FileLocation.of(pom));
  }

  public boolean isDebugLogs() {
    return debugLogs;
  }

  public MavenBuild setDebugLogs(boolean b) {
    this.debugLogs = b;
    return this;
  }

  public File getExecutionDir() {
    return executionDir;
  }

  public List<String> getGoals() {
    return goals;
  }

  public MavenBuild addGoal(String goal) {
    checkArgument(!isEmpty(goal), "Maven goal must be set");
    this.goals.add(goal);
    return this;
  }

  public MavenBuild addSonarGoal() {
    return addGoal("sonar:sonar");
  }

  /**
   * mvn clean package && mvn sonar:sonar
   */
  public MavenBuild setCleanPackageSonarGoals() {
    return setGoals("clean package", "sonar:sonar");
  }

  /**
   * mvn clean sonar:sonar
   */
  public MavenBuild setCleanSonarGoals() {
    return setGoals("clean sonar:sonar");
  }

  public MavenBuild setGoals(String... goals) {
    return setGoals(Arrays.asList(goals));
  }

  public MavenBuild setGoals(List<String> goals) {
    checkArgument(!goals.isEmpty(), "At least one goal must be set");
    this.goals = goals;
    return this;
  }

  public MavenBuild setExecutionDir(File executionDir) {
    this.executionDir = executionDir;
    return this;
  }

  public MavenBuild setDynamicAnalysis(boolean dynamicAnalysis) {
    getProperties().put("sonar.dynamicAnalysis", String.valueOf(dynamicAnalysis));
    return this;
  }

  public static MavenBuild create() {
    return new MavenBuild();
  }

  public static MavenBuild create(Location pom) {
    return new MavenBuild().setPom(pom);
  }

  public static MavenBuild create(File pom) {
    return new MavenBuild().setPom(pom);
  }

  @Override
  BuildResult execute(Configuration config, Map<String, String> adjustedProperties) {
    return new MavenBuildExecutor().execute(this, config, adjustedProperties);
  }
}
