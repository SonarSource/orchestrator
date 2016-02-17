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
import com.sonar.orchestrator.config.Configuration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "create-db", requiresDependencyResolution = ResolutionScope.RUNTIME, requiresProject = false, threadSafe = true)
public class CreateDbMojo extends AbstractMojo {

  @Parameter(property = "session", required = true, readonly = true)
  private MavenSession session;

  @Parameter(property = "sonar.runtimeVersion", required = false)
  protected String sqVersion = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Configuration.Builder configBuilder = Configuration.builder();
    configBuilder.addEnvVariables();
    configBuilder.addMap(session.getExecutionProperties());
    if (sqVersion != null && sqVersion.trim().length() > 0) {
      configBuilder.setProperty("sonar.runtimeVersion", sqVersion);
    }
    configBuilder.setProperty("maven.localRepository", session.getLocalRepository().getBasedir());

    Orchestrator orchestrator = Orchestrator.builder(configBuilder.build()).build();
    try {
      orchestrator.start();
    } finally {
      orchestrator.stop();
    }
  }
}
