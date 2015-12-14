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
package com.sonar.orchestrator.dsl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;

import java.io.File;
import java.util.Map;

public class SonarRunnerCommand extends Command {

  private Map<String, String> properties = Maps.newHashMap();

  public void setProperty(String key, String value) {
    properties.put(key, value);
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public void execute(Dsl.Context context) {
    File dir = (File) context.getState(CdCommand.PWD_KEY);
    Orchestrator orchestrator = context.getOrchestrator();

    Preconditions.checkState(orchestrator != null, "The command 'start server' has not been executed.");
    Preconditions.checkState(dir != null, "The command 'cd' has not been executed.");

    SonarRunner build = SonarRunner.create(dir);
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      build.setProperty(entry.getKey(), entry.getValue());
    }
    executeBuild(context, build);
  }

  void executeBuild(Dsl.Context context, SonarRunner build) {
    context.getOrchestrator().executeBuild(build);
  }
}
