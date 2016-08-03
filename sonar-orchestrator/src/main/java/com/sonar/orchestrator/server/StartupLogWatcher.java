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
package com.sonar.orchestrator.server;

/**
 * @see com.sonar.orchestrator.OrchestratorBuilder#setStartupLogWatcher(StartupLogWatcher)
 * @since 3.13
 */
@FunctionalInterface
public interface StartupLogWatcher {

  /**
   *
   * @param logLine a line of logs/sonar.log
   * @return true if server is detected as started and operational, otherwise false
   */
  boolean isStarted(String logLine);

}
