/*
 * Orchestrator
 * Copyright (C) 2011-2023 SonarSource SA
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

public class MsSqlTest {

  @Test
  public void testDefaultConfiguration() {
    MsSql mssql = MsSql.jtdsBuilder().build();
    assertThat(mssql.getDialect()).isEqualTo("mssql");
    assertThat(mssql.getDriverClassName()).isEqualTo("net.sourceforge.jtds.jdbc.Driver");
    assertThat(mssql.getUrl()).isEqualTo("jdbc:jtds:sqlserver://localhost/sonar;SelectMethod=Cursor");
    assertThat(mssql.getRootUrl()).isEqualTo("jdbc:jtds:sqlserver://localhost");
    assertThat(mssql.getLogin()).isEqualTo("sonar");
    assertThat(mssql.getPassword()).isEqualTo("sonar");
    assertThat(mssql.isDropAndCreate()).isEqualTo(true);
  }

  @Test
  public void rootUrlShouldEqualUrl() {
    DatabaseClient mssql = MsSql.jtdsBuilder().setUrl("jdbc:jtds:sqlserver://localhost").build();
    assertThat(mssql.getUrl()).isEqualTo("jdbc:jtds:sqlserver://localhost");
    assertThat(mssql.getRootUrl()).isEqualTo(mssql.getUrl());
  }

  @Test
  public void testBuilder() {
    MsSql mssql = MsSql.jtdsBuilder()
      .setDropAndCreate(false)
      .setDriverClassName("org.mssql.OtherDriver")
      .setRootLogin("hello").setRootPassword("world")
      .build();
    assertThat(mssql.getDialect()).isEqualTo("mssql");
    assertThat(mssql.getDriverClassName()).isEqualTo("org.mssql.OtherDriver");
    assertThat(mssql.isDropAndCreate()).isEqualTo(false);
    assertThat(mssql.getRootLogin()).isEqualTo("hello");
    assertThat(mssql.getRootPassword()).isEqualTo("world");
  }

  @Test
  public void ddlShouldBeDefined() {
    MsSql mssql = MsSql.jtdsBuilder().build();
    assertThat(mssql.getDropDdl().length).isGreaterThan(0);
    assertThat(mssql.getCreateDdl().length).isGreaterThan(0);
  }

  @Test
  public void testListAndKillProcesses() {
    MsSql mssql = MsSql.jtdsBuilder().build();
    assertThat(mssql.getSelectConnectionIdsSql()).contains("SELECT");
    assertThat(mssql.getKillConnectionSql("007")).contains("KILL");
  }

}
