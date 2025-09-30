/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BuildResult {

  // StringWriter does not need to be closed
  private StringWriter logs = new StringWriter();
  private List<Integer> statuses = new ArrayList<>();

  public Writer getLogsWriter() {
    return logs;
  }

  public String getLogs() {
    return logs.toString();
  }

  public List<String> getLogsLines(Predicate<String> linePredicate) {
    return Arrays.stream(logs.toString().split("\r?\n|\r"))
      .filter(linePredicate)
      .toList();
  }

  /**
   * @deprecated since 3.13 use {@link #getLastStatus()}
   */
  @Deprecated
  public Integer getStatus() {
    return getLastStatus();
  }

  /**
   * With {@link MavenBuild} or {@link AntBuild} it is possible to chain execution of several goals/target. 
   * This will return the status of the last execution.
   * @since 3.13
   */
  public Integer getLastStatus() {
    return statuses.isEmpty() ? null : statuses.get(statuses.size() - 1);
  }

  public List<Integer> getStatuses() {
    return Collections.unmodifiableList(statuses);
  }

  public BuildResult addStatus(Integer status) {
    statuses.add(status);
    return this;
  }

  /**
   * Tests statuses, return true if all zero, else false
   * @return true if all statuses are zero
   */
  public boolean isSuccess() {
    return statuses.stream().allMatch(s -> s == 0);
  }
}
