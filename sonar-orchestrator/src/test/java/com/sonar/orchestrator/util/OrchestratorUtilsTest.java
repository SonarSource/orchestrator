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
package com.sonar.orchestrator.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfEmpty;
import static com.sonar.orchestrator.util.OrchestratorUtils.defaultIfNull;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;

public class OrchestratorUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_isEmpty() {
    assertThat(isEmpty(null)).isTrue();
    assertThat(isEmpty("")).isTrue();
    assertThat(isEmpty("  ")).isFalse();
    assertThat(isEmpty("foo")).isFalse();
  }

  @Test
  public void checkArgument_does_not_throw_IAE_if_expression_is_true() {
    checkArgument(true, "foo");
  }

  @Test
  public void checkArgument_throws_IAE_if_expression_is_false() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("foo");

    checkArgument(false, "foo");
  }

  @Test
  public void checkArgument_throws_IAE_with_formatted_message_if_expression_is_false() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("hello:world");

    checkArgument(false, "%s:%s", "hello", "world");
  }

  @Test
  public void checkState_does_not_throw_ISE_if_expression_is_true() {
    checkState(true, "foo");
  }

  @Test
  public void checkState_throws_ISE_if_expression_is_false() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("foo");

    checkState(false, "foo");
  }

  @Test
  public void checkState_throws_ISE_with_formatted_message_if_expression_is_false() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("hello:world");

    checkState(false, "%s:%s", "hello", "world");
  }

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
