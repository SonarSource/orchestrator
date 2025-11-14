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

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OracleTest {

  private final File driverFile = FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/db/OracleTest/oracle-driver.jar"));

  @Test
  public void testDefaultConfiguration() {
    Oracle oracle = Oracle.builder().setDriverFile(driverFile).build();
    assertThat(oracle.getDialect()).isEqualTo("oracle");
    assertThat(oracle.getDriverClassName()).isEqualTo("oracle.jdbc.OracleDriver");
    assertThat(oracle.getUrl()).isEqualTo("jdbc:oracle:thin:@localhost/XE");
    assertThat(oracle.getRootUrl()).isEqualTo("jdbc:oracle:thin:@localhost/XE");// same as url
    assertThat(oracle.getLogin()).isEqualTo("sonar");
    assertThat(oracle.getPassword()).isEqualTo("sonar");
    assertThat(oracle.isDropAndCreate()).isEqualTo(true);
  }

  @Test
  public void testBuilder() {
    Oracle oracle = Oracle.builder()
      .setDropAndCreate(false)
      .setDriverClassName("org.oracle.OtherDriver")
      .setRootLogin("hello").setRootPassword("world")
      .setDriverFile(driverFile)
      .build();
    assertThat(oracle.getDialect()).isEqualTo("oracle");
    assertThat(oracle.getDriverClassName()).isEqualTo("org.oracle.OtherDriver");
    assertThat(oracle.isDropAndCreate()).isEqualTo(false);
    assertThat(oracle.getRootLogin()).isEqualTo("hello");
    assertThat(oracle.getRootPassword()).isEqualTo("world");
  }

  @Test
  public void ddlShouldBeDefined() {
    Oracle oracle = Oracle.builder().setDriverFile(driverFile).build();
    assertThat(oracle.getCreateDdl().length).isGreaterThan(0);
    assertThat(oracle.getDropDdl().length).isGreaterThan(0);
  }

  @Test
  public void testListAndKillProcesses() {
    Oracle oracle = Oracle.builder().setDriverFile(driverFile).build();
    assertThat(oracle.getSelectConnectionIdsSql()).contains("SELECT");
    assertThat(oracle.getKillConnectionSql("007,022")).isEqualTo("ALTER SYSTEM DISCONNECT SESSION '007,022' IMMEDIATE");
  }

  @Test(expected = IllegalArgumentException.class)
  public void pathToDriverShouldBeMandatory() {
    Oracle.builder().build();
  }

  @Test
  public void noDefaultValueForSchemaProperty() {
    Oracle oracle = Oracle.builder().setDriverFile(driverFile).build();
    assertThat(oracle.getSchema()).isNull();
  }
}
