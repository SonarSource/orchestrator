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

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.Location;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class AntBuild extends Build<AntBuild> {

  private File antHome;
  private Location buildLocation;
  private List<String> targets;

  public File getAntHome() {
    return antHome;
  }

  public Location getBuildLocation() {
    return buildLocation;
  }

  public List<String> getTargets() {
    return targets;
  }

  public AntBuild setAntHome(File antHome) {
    this.antHome = antHome;
    return this;
  }

  public AntBuild setBuildLocation(Location buildLocation) {
    this.buildLocation = buildLocation;
    return this;
  }

  public AntBuild setTargets(List<String> targets) {
    this.targets = targets;
    return this;
  }

  public AntBuild addTarget(String s) {
    if (this.targets == null) {
      this.targets = new ArrayList<>();
    }
    this.targets.add(s);
    return this;
  }

  public AntBuild setTargets(String... targets) {
    this.targets = Arrays.asList(targets);
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("AntBuild{");
    sb.append("antHome=").append(antHome);
    sb.append(", buildLocation=").append(buildLocation);
    sb.append(", targets=").append(targets);
    sb.append('}');
    return sb.toString();
  }

  @Override
  BuildResult execute(Configuration config, Map<String, String> adjustedProperties) {
    return new AntBuildExecutor().execute(this, config, adjustedProperties);
  }

  public static AntBuild create() {
    return new AntBuild();
  }

  public static AntBuild create(Location buildFile, String[] targets, String... keyValueProperties) {
    return new AntBuild().setBuildLocation(buildFile).setTargets(targets).setProperties(keyValueProperties);
  }
}
