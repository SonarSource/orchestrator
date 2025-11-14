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
package com.sonar.orchestrator.db;

import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseClientTest {
  @Test
  public void testGetProperties() {
    H2 h2 = H2.builder().setDriverClassName("my.Driver").build();
    Map<String, String> props = h2.getProperties();
    assertThat(props)
      .containsEntry("sonar.jdbc.dialect", "h2")
      .containsEntry("sonar.jdbc.username", "sonar")
      .containsEntry("sonar.jdbc.password", "sonar")
      .containsEntry("sonar.jdbc.driverClassName", "my.Driver");
    assertThat(h2.isDropAndCreate()).isFalse();
  }

  @Test
  public void testSetProperties() {
    H2 h2 = H2.builder().setDriverClassName("my.Driver").build();

    // set
    h2.setDropAndCreate(true);

    // checks
    assertThat(h2.isDropAndCreate()).isTrue();
  }

  @Test
  public void testGetDbMetatdata() {
    H2 h2 = H2.builder().setDriverClassName("my.Driver").build();
    try {
      h2.openConnection();
    } catch (Exception devnull) {
      // Ignore exception
    }
    assertThat(h2.getDBMajorVersion()).isGreaterThanOrEqualTo(0);
    assertThat(h2.getDBMinorVersion()).isGreaterThanOrEqualTo(0);
  }

  @Test
  public void testDefaultGetPermissionOnSchema() {
    H2 h2 = H2.builder().setDriverClassName("my.Driver").build();
    assertThat(h2.getPermissionOnSchema()).isEmpty();
  }
}
