/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource Sàrl
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfEmpty;
import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfNull;
import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_defaultIfEmpty() {
    assertThat(defaultIfEmpty("foo", "bar")).isEqualTo("foo");
    assertThat(defaultIfEmpty("  ", "bar")).isEqualTo("  ");
    assertThat(defaultIfEmpty("", "bar")).isEqualTo("bar");
    assertThat(defaultIfEmpty("", null)).isNull();
    assertThat(defaultIfEmpty("", "")).isEqualTo("");
    assertThat(defaultIfEmpty(null, "bar")).isEqualTo("bar");
    assertThat(defaultIfEmpty(null, null)).isNull();
    assertThat(defaultIfEmpty(null, "")).isEqualTo("");
  }

  @Test
  public void test_defaultIfNull() {
    assertThat(defaultIfNull("foo", "bar")).isEqualTo("foo");
    assertThat(defaultIfNull("", "bar")).isEqualTo("");
    assertThat(defaultIfNull(" ", "bar")).isEqualTo(" ");
    assertThat(defaultIfNull(null, "bar")).isEqualTo("bar");
    assertThat(defaultIfNull(3L, 4L)).isEqualTo(3L);
    assertThat(defaultIfNull(null, 4L)).isEqualTo(4L);
  }
}
