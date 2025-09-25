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
package com.sonar.orchestrator.util;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class SharedDir {
  private static final String ENV_SHARED_DIR = "SONAR_IT_SOURCES";
  private static final String PROP_SHARED_DIR = "orchestrator.it_sources";

  private final Configuration configuration;

  public SharedDir(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * File located in the shared directory defined by the system property orchestrator.it_sources or environment variable SONAR_IT_SOURCES.
   * Example : getFileLocationOfShared("javascript/performancing/pom.xml")
   */
  public FileLocation getFileLocationOfShared(String relativePath) {
    // try to read it_sources
    // in the System.getProperties
    // in the prop file (from orchestrator.properties file)
    // in the environment variable
    String rootPath;
    rootPath = System.getProperty(PROP_SHARED_DIR);
    if (rootPath == null) {
      rootPath = configuration.getString(PROP_SHARED_DIR);
    }
    if (rootPath == null) {
      rootPath = System.getenv(ENV_SHARED_DIR);
    }
    requireNonNull(rootPath, format("Property '%s' or environment variable '%s' is missing", PROP_SHARED_DIR, ENV_SHARED_DIR));

    File rootDir = new File(rootPath);
    checkState(rootDir.isDirectory() && rootDir.exists(),
      "Please check the definition of it_sources (%s or %s) because the directory does not exist: %s", PROP_SHARED_DIR, ENV_SHARED_DIR, rootDir);

    return FileLocation.of(new File(rootDir, relativePath));
  }

}
