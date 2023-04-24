/*
 * Orchestrator - JUnit 5
 * Copyright (C) 2011-2023 SonarSource SA
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
package com.sonar.orchestrator.junit5;

import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.server.StartupLogWatcher;
import com.sonar.orchestrator.util.System2;

public class OrchestratorExtensionBuilder extends OrchestratorBuilder<OrchestratorExtensionBuilder, OrchestratorExtension> {
  OrchestratorExtensionBuilder(Configuration initialConfig) {
    this(initialConfig, System2.INSTANCE);
  }

  OrchestratorExtensionBuilder(Configuration initialConfig, System2 system2) {
    super(initialConfig, system2);
  }

  @Override
  protected OrchestratorExtension build(Configuration finalConfig, SonarDistribution distribution, StartupLogWatcher startupLogWatcher) {
    return new OrchestratorExtension(finalConfig, distribution, startupLogWatcher);
  }

}
