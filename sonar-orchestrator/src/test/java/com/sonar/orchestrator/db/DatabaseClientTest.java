/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
    assertThat(props.get("sonar.jdbc.dialect")).isEqualTo("h2");
    assertThat(props.get("sonar.jdbc.username")).isEqualTo("sonar");
    assertThat(props.get("sonar.jdbc.password")).isEqualTo("sonar");
    assertThat(props.get("sonar.jdbc.driverClassName")).isEqualTo("my.Driver");
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
    try{ h2.openConnection(); } catch( Exception devnull ) {}
    assertThat(h2.getDBMajorVersion() >= 0).isTrue();
    assertThat(h2.getDBMinorVersion() >= 0).isTrue();
  }

  @Test
  public void testDefaultGetPermissionOnSchema() {
    H2 h2 = H2.builder().setDriverClassName("my.Driver").build();
    assertThat(h2.getPermissionOnSchema()).isEmpty();
  }
}
