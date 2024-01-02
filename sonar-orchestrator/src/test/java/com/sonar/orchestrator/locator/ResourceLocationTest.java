/*
 * Orchestrator
 * Copyright (C) 2011-2024 SonarSource SA
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
package com.sonar.orchestrator.locator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceLocationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void path_must_start_with_slash() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Path must start with slash");

    ResourceLocation.create("path");
  }

  @Test
    public void test_valid_path() {
    ResourceLocation location = ResourceLocation.create("/com/sonar/orchestrator/locator/ResourceLocationTest/foo.txt");

    assertThat(location.getFilename()).isEqualTo("foo.txt");
    assertThat(location.getPath()).isEqualTo("/com/sonar/orchestrator/locator/ResourceLocationTest/foo.txt");
    assertThat(location.toString()).isEqualTo(location.getPath());
  }
}
