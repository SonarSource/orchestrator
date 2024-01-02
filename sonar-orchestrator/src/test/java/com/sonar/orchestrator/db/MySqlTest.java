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
package com.sonar.orchestrator.db;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MySqlTest {

  @Test
  public void testDefaultConfiguration() {
    MySql mysql = MySql.builder().build();
    assertThat(mysql.getDialect()).isEqualTo("mysql");
    assertThat(mysql.getDriverClassName()).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(mysql.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8");
    assertThat(mysql.getLogin()).isEqualTo("sonar");
    assertThat(mysql.getPassword()).isEqualTo("sonar");
    assertThat(mysql.isDropAndCreate()).isEqualTo(true);
    assertThat(mysql.getRootLogin()).isEqualTo("root");
    assertThat(mysql.getRootPassword()).isEqualTo("");
  }

  @Test
  public void testBuilder() {
    MySql mysql = MySql.builder()
      .setUrl("jdbc:mysql://localhost:4890/sonar")
      .setRootUrl("jdbc:mysql://localhost:4890")
      .setDropAndCreate(false)
      .setRootLogin("hello").setRootPassword("world")
      .build();
    assertThat(mysql.getDialect()).isEqualTo("mysql");
    assertThat(mysql.getDriverClassName()).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(mysql.getUrl()).isEqualTo("jdbc:mysql://localhost:4890/sonar");
    assertThat(mysql.getLogin()).isEqualTo("sonar");
    assertThat(mysql.getPassword()).isEqualTo("sonar");

    assertThat(mysql.isDropAndCreate()).isEqualTo(false);
    assertThat(mysql.getRootUrl()).isEqualTo("jdbc:mysql://localhost:4890");
    assertThat(mysql.getRootLogin()).isEqualTo("hello");
    assertThat(mysql.getRootPassword()).isEqualTo("world");
  }

  @Test
  public void ddlShouldBeDefined() {
    MySql mysql = MySql.builder().build();
    assertThat(mysql.getCreateDdl().length).isGreaterThan(0);
    assertThat(mysql.getDropDdl().length).isGreaterThan(0);
  }

  @Test
  public void testListAndKillProcesses() {
    MySql mysql = MySql.builder().build();
    assertThat(mysql.getSelectConnectionIdsSql()).contains("SELECT");
    assertThat(mysql.getKillConnectionSql("007")).contains("KILL");
  }

  @Test
  public void defaultValueOfSchemaIsSonar() {
    MySql mysql = MySql.builder().build();
    assertThat(mysql.getSchema()).isEqualTo("sonar");
    assertThat(mysql.getDropDdl().length).isGreaterThan(0);
  }
}
