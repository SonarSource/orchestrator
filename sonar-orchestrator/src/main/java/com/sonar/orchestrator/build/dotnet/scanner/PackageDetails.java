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
package com.sonar.orchestrator.build.dotnet.scanner;

import org.jetbrains.annotations.NotNull;

public class PackageDetails {
  private final String groupId;
  private final String artifactId;
  private final String classifier;
  private final String packageName;
  private final String executableName;

  public PackageDetails(@NotNull String groupId, @NotNull String artifactId, @NotNull String classifier, @NotNull String packageName, @NotNull String executableName) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.classifier = classifier;
    this.packageName = packageName;
    this.executableName = executableName;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getExecutableName() {
    return executableName;
  }

  public String getClassifier() {
    return classifier;
  }
}
