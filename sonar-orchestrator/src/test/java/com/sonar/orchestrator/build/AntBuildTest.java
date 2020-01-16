/*
 * Orchestrator
 * Copyright (C) 2011-2020 SonarSource SA
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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AntBuildTest {
  @Test
  public void test_builder() {
    File antHome = new File("target/ant");
    AntBuild build = new AntBuild()
      .setAntHome(antHome)
      .setBuildLocation(FileLocation.of("."))
      .addTarget("compile")
      .addTarget("sonar")
      .setTimeoutSeconds(50000L)
      .setProperty("foo", "bar");

    assertThat(build.getAntHome()).isEqualTo(antHome);
    assertThat(build.getBuildLocation()).isEqualTo(FileLocation.of(new File(".")));
    assertThat(build.getTargets()).containsExactly("compile", "sonar");
    assertThat(build.getTimeoutSeconds()).isEqualTo(50000L);
    assertThat(build.getProperties()).hasSize(1);
    assertThat(build.getProperties().get("foo")).isEqualTo("bar");
    assertThat(build.toString()).contains("compile").contains("sonar");
  }

  @Test
  public void test_create() {
    AntBuild build = AntBuild.create();

    build.setTargets("libA", "libB", "libC");
    assertThat(build.getTargets()).containsExactly("libA", "libB", "libC");
  }
}
