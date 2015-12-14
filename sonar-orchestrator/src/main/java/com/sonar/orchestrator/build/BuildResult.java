/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

public class BuildResult {

  // StringWriter does not need to be closed
  private StringWriter logs = new StringWriter();
  private Integer status;

  public Writer getLogsWriter() {
    return logs;
  }

  public String getLogs() {
    return logs.toString();
  }

  public Integer getStatus() {
    return status;
  }

  public BuildResult setStatus(Integer status) {
    this.status = status;
    return this;
  }

  /**
   * Tests status, return true if zero, false
   * @return true if the status is zero
   */
  public boolean isSuccess() {
    return getStatus() == 0;
  }
}
