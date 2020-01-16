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

import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertyUtilsTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void properties_array_to_map() {
    String[] props = {"key1", "value1", "key2", "value2"};

    Map<String, String> map = PropertyUtils.toMap(props);

    assertThat(map).hasSize(2);
    assertThat(map.get("key1")).isEqualTo("value1");
    assertThat(map.get("key2")).isEqualTo("value2");
  }

  @Test
  public void must_be_even_number_of_properties() {
    thrown.expect(IllegalArgumentException.class);
    PropertyUtils.toMap(new String[]{"key1", "value1", "key2"});
  }
}
