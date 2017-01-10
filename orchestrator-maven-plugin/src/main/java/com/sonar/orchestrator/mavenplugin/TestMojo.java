/*
 * Orchestrator :: Maven Plugin
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
package com.sonar.orchestrator.mavenplugin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.sonar.orchestrator.dsl.Dsl;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @goal test
 * @requiresProject false
 */
public class TestMojo extends AbstractMojo {

  /**
   * @parameter property="dir" default-value="${basedir}"
   */
  File dir;

  /**
   * @parameter
   */
  String includes = "**" + File.separator + "validation.txt";

  /**
   * @parameter
   */
  String excludes;

  /**
   * Encoding of the source files.
   *
   * @parameter property="encoding" default-value="UTF-8"
   */
  private String encoding;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Dsl dsl = new Dsl();
    doExecute(dsl);
  }

  @VisibleForTesting
  void doExecute(Dsl dsl) throws MojoExecutionException {
    for (File file : getFiles()) {
      try {
        getLog().info("Executing " + file);
        String dslText = FileUtils.readFileToString(file, encoding);
        Dsl.Context context = new Dsl.Context().setSettings(Maps.fromProperties(System.getProperties()));
        context.setState("pwd", file.getParentFile());
        dsl.execute(dslText, context);
      } catch (Exception e) {
        throw new MojoExecutionException("Fail to execute " + file, e);
      }
    }
  }

  private List<File> getFiles() throws MojoExecutionException {
    try {
      return org.codehaus.plexus.util.FileUtils.getFiles(dir, includes, excludes);
    } catch (IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

}
