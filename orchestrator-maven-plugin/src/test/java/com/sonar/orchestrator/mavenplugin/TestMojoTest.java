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

import com.sonar.orchestrator.dsl.Dsl;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestMojoTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_execute_scripts() throws Exception {
    Dsl dsl = mock(Dsl.class);

    TestMojo mojo = new TestMojo();
    mojo.dir = scriptDir();
    mojo.includes = "**/TestMojoTest/script*.txt";
    mojo.doExecute(dsl);

    verify(dsl).execute(eq("# First script"), any(Dsl.Context.class));
    verify(dsl).execute(eq("# Second script"), any(Dsl.Context.class));
  }

  @Test
  public void should_fail() throws Exception {
    thrown.expect(MojoExecutionException.class);

    Dsl dsl = mock(Dsl.class);
    doThrow(new IllegalStateException()).when(dsl).execute(anyString(), any(Dsl.Context.class));

    TestMojo mojo = new TestMojo();
    mojo.dir = scriptDir();
    mojo.includes = "**/TestMojoTest/script*.txt";
    mojo.doExecute(dsl);
  }

  private File scriptDir() throws URISyntaxException {
    File file = new File(TestMojoTest.class.getResource("/com/sonar/orchestrator/mavenplugin/TestMojoTest/script1.txt").toURI());
    return file.getParentFile().getParentFile();
  }
}
