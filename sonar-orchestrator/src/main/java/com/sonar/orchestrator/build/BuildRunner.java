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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.db.Database;

import java.util.Map;

public class BuildRunner {

  private final Configuration config;
  private final Database database;

  public BuildRunner(Configuration config, Database database) {
    this.config = config;
    this.database = database;
  }

  public BuildResult runQuietly(Server server, Build<?> build) {
    return build.execute(config, adjustProperties(server, build));
  }

  public BuildResult run(Server server, Build<?> build) {
    BuildResult result = runQuietly(server, build);
    if (!result.isSuccess()) {
      throw new BuildFailureException(build, result);
    }
    return result;
  }

  @VisibleForTesting
  Map<String, String> adjustProperties(Server server, Build<?> build) {
    Map<String, String> adjustedProperties = Maps.newHashMap();
    if (server != null) {
      adjustedProperties.put("sonar.host.url", server.getUrl());
      adjustedProperties.putAll(database.getSonarProperties());
    }
    // build properties override predefined properties
    adjustedProperties.putAll(build.getProperties());

    return adjustedProperties;
  }

}
