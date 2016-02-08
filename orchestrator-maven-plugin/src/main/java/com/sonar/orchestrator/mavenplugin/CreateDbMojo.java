/*
 * Orchestrator :: Maven Plugin
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
package com.sonar.orchestrator.mavenplugin;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.config.Configuration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @goal create-db
 * @requiresProject false
 * @requiresDependencyResolution runtime
 */
public class CreateDbMojo extends AbstractMojo {

  /**
   * @parameter property="session"
   * @readonly
   * @required
   */
  protected MavenSession session;

  @Parameter(property = "sonar.runtimeVersion", required = false)
  protected String sqVersion = null;


  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Configuration config = Configuration.builder()
      .addEnvVariables()
      .addMap(session.getExecutionProperties())
      .build();

    OrchestratorBuilder builder = Orchestrator.builder(config);
    if (sqVersion != null && sqVersion.trim().length() > 0) {
      builder.setOrchestratorProperty("sonar.runtimeVersion", sqVersion);
    }
    Orchestrator orchestrator = builder.build();
    try {
      orchestrator.start();
    } finally {
      orchestrator.stop();
    }
  }
}
