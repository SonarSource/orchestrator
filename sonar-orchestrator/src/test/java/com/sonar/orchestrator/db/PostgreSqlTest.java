/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgreSqlTest {

  @Test
  public void testDefaultConfiguration() {
    PostgreSql postgreSql = PostgreSql.builder().build();
    assertThat(postgreSql.getDialect()).isEqualTo("postgresql");
    assertThat(postgreSql.getDriverClassName()).isEqualTo("org.postgresql.Driver");
    assertThat(postgreSql.getUrl()).isEqualTo("jdbc:postgresql://localhost/sonar");
    assertThat(postgreSql.getLogin()).isEqualTo("sonar");
    assertThat(postgreSql.getPassword()).isEqualTo("sonar");
    assertThat(postgreSql.isDropAndCreate()).isTrue();
  }

  @Test
  public void testBuilder() {
    PostgreSql postgreSql = PostgreSql.builder()
      .setDropAndCreate(false)
      .setRootLogin("hello")
      .setRootPassword("world")
      .build();
    assertThat(postgreSql.getDialect()).isEqualTo("postgresql");
    assertThat(postgreSql.getDriverClassName()).isEqualTo("org.postgresql.Driver");
    assertThat(postgreSql.getUrl()).isEqualTo("jdbc:postgresql://localhost/sonar");
    assertThat(postgreSql.getLogin()).isEqualTo("sonar");
    assertThat(postgreSql.getPassword()).isEqualTo("sonar");

    assertThat(postgreSql.isDropAndCreate()).isFalse();
    assertThat(postgreSql.getRootUrl()).isEqualTo("jdbc:postgresql://localhost");
    assertThat(postgreSql.getRootLogin()).isEqualTo("hello");
    assertThat(postgreSql.getRootPassword()).isEqualTo("world");
  }

  @Test
  public void ddlShouldBeDefined() {
    PostgreSql postgreSql = PostgreSql.builder().build();
    assertThat(postgreSql.getCreateDdl().length).isGreaterThan(0);
    assertThat(postgreSql.getDropDdl().length).isGreaterThan(0);

  }

  @Test
  public void createdDdlShouldBeDefined() {
    PostgreSql postgreSql = PostgreSql.builder().build();
    String[] expectedSqlCommand = {
      "create database \"sonar\"",
      "CREATE USER \"sonar\" WITH PASSWORD 'sonar' CREATEDB",
      "GRANT ALL PRIVILEGES ON DATABASE \"sonar\" TO \"sonar\""
    };
    assertThat(Arrays.equals(postgreSql.getCreateDdl(), expectedSqlCommand)).isTrue();
  }

  @Test
  public void permissionOnSchemaShouldBeDefined() {
    PostgreSql postgreSql = PostgreSql.builder().build();
    String[] expectedSqlCommand = { "GRANT CREATE, USAGE ON SCHEMA public TO \"sonar\";" };
    assertThat(Arrays.equals(postgreSql.getPermissionOnSchema(), expectedSqlCommand)).isTrue();
  }

  @Test
  public void testListAndKillProcesses() {
    PostgreSql postgreSql = PostgreSql.builder().build();
    assertThat(postgreSql.getSelectConnectionIdsSql()).contains("SELECT");
    assertThat(postgreSql.getKillConnectionSql("007")).contains("pg_terminate_backend");
  }
}
