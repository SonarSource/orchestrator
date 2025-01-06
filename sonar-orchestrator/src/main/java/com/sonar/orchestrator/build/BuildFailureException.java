/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
