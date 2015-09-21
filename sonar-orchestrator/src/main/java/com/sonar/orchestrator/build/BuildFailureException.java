/*
 * Orchestrator
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.orchestrator.build;

import static com.google.common.base.Preconditions.checkNotNull;

public class BuildFailureException extends RuntimeException {
  private final Build build;
  private final BuildResult result;

  public BuildFailureException(Build<?> build, BuildResult result) {
    this.build = checkNotNull(build, "build is null");
    this.result = checkNotNull(result, "result is null");
  }

  public Build getBuild() {
    return build;
  }

  public BuildResult getResult() {
    return result;
  }

  @Override
  public String getMessage() {
    return "status=" + result.getStatus() + " " + "build=[" + build + "]";
  }
}
