/*
 * Orchestrator Utils
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

import org.junit.Test;

import static com.sonar.orchestrator.util.Preconditions.checkArgument;
import static com.sonar.orchestrator.util.Preconditions.checkState;
import static org.junit.Assert.assertThrows;

public class PreconditionsTest {

  @Test
  public void checkArgument_does_not_throw_IAE_if_expression_is_true() {
    checkArgument(true, "foo");
  }

  @Test
  public void checkArgument_throws_IAE_if_expression_is_false() {
    assertThrows("foo", IllegalArgumentException.class,
      () -> checkArgument(false, "foo"));
  }

  @Test
  public void checkArgument_throws_IAE_with_formatted_message_if_expression_is_false() {
    assertThrows("hello:world", IllegalArgumentException.class,
      () -> checkArgument(false, "%s:%s", "hello", "world"));
  }

  @Test
  public void checkState_does_not_throw_ISE_if_expression_is_true() {
    checkState(true, "foo");
  }

  @Test
  public void checkState_throws_ISE_if_expression_is_false() {
    assertThrows("foo", IllegalStateException.class,
      () -> checkState(false, "foo"));
  }

  @Test
  public void checkState_throws_ISE_with_formatted_message_if_expression_is_false() {
    assertThrows("hello:world", IllegalStateException.class,
      () -> checkState(false, "%s:%s", "hello", "world"));
  }
}
