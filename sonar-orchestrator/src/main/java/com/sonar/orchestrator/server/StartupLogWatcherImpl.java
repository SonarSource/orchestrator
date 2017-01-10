/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.version.Version;

class StartupLogWatcherImpl implements StartupLogWatcher {
  private final String startupExpectedMessage;

  private StartupLogWatcherImpl(String startupExpectedMessage) {
    this.startupExpectedMessage = startupExpectedMessage;
  }

  @Override
  public boolean isStarted(String logLine) {
    return logLine.contains(startupExpectedMessage);
  }

  static StartupLogWatcherImpl create(Version serverVersion) {
    String startupExpectedMessage;
    if (serverVersion.isGreaterThanOrEquals("5.5")) {
      startupExpectedMessage = "Process[ce] is up";
    } else {
      startupExpectedMessage = "Process[web] is up";
    }
    return new StartupLogWatcherImpl(startupExpectedMessage);
  }
}
