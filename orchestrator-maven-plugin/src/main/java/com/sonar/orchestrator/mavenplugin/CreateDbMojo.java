/*
 * Orchestrator :: Maven Plugin
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
package com.sonar.orchestrator.mavenplugin;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.config.Configuration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Configuration config = Configuration.builder()
      .addEnvVariables()
      .addMap(session.getExecutionProperties())
      .build();

    Orchestrator orchestrator = Orchestrator.builder(config).build();
    try {
      orchestrator.start();
    } finally {
      orchestrator.stop();
    }
  }
}
