/*
 * Orchestrator
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

import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class BuildFailureException extends RuntimeException {
  private final transient Build build;
  private final transient BuildResult result;

  public BuildFailureException(Build<?> build, BuildResult result) {
    this.build = requireNonNull(build, "build is null");
    this.result = requireNonNull(result, "result is null");
  }

  public Build getBuild() {
    return build;
  }

  public BuildResult getResult() {
    return result;
  }

  @Override
  public String getMessage() {
    return "statuses=[" +
      result.getStatuses().stream()
        .map(Object::toString)
        .collect(Collectors.joining(", "))
      + "] logs=[" + result.getLogs() + "]";
  }
}
