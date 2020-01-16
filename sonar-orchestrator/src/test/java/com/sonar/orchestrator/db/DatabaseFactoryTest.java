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
package com.sonar.orchestrator.db;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseFactoryTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void shouldUseDatabaseProperties() {
    File driverFile = getDriverFile();
    Configuration config = Configuration.builder()
      .setProperty("sonar.jdbc.url", "jdbc:mysql://localhost:3306/sonar")
      .setProperty("sonar.jdbc.username", "user")
      .setProperty("sonar.jdbc.password", "password")
      .setProperty("sonar.jdbc.rootUsername", "new_root")
      .setProperty("sonar.jdbc.rootPassword", "new_pass")
      .setProperty("sonar.jdbc.driverFile", driverFile)
      .setProperty("orchestrator.keepDatabase", "yes")
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(config, mock(Locators.class));

    assertThat(databaseClient.getDialect()).isEqualTo("mysql");
    assertThat(databaseClient.getLogin()).isEqualTo("user");
    assertThat(databaseClient.getPassword()).isEqualTo("password");
    assertThat(databaseClient.getRootLogin()).isEqualTo("new_root");
    assertThat(databaseClient.getRootPassword()).isEqualTo("new_pass");
    assertThat(databaseClient.getDriverClassName()).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(databaseClient.getDriverFile()).isEqualTo(driverFile);
  }

  @Test
  public void should_support_h2() {
    Configuration config = Configuration.builder()
      .setProperty("sonar.jdbc.url", "jdbc:h2:tcp://localhost:9092/sonar")
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(config, mock(Locators.class));

    assertThat(databaseClient.getDialect()).isEqualTo("h2");
  }

  @Test
  public void default_db_is_h2() {
    Configuration config = Configuration.builder()
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(config, mock(Locators.class));

    assertThat(databaseClient.getDialect()).isEqualTo("h2");
  }

  @Test
  public void should_support_postgresql() {
    Configuration config = Configuration.builder()
      .setProperty("sonar.jdbc.url", "jdbc:postgresql://localhost/sonar")
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(config, mock(Locators.class));

    assertThat(databaseClient.getDialect()).isEqualTo("postgresql");
  }

  @Test
  public void should_support_mssql_with_microsoft_driver() {
    Configuration config = Configuration.builder()
      .setProperty("sonar.jdbc.url", "jdbc:sqlserver://localhost;databaseName=sonar")
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(config, mock(Locators.class));

    assertThat(databaseClient.getDialect()).isEqualTo("mssql");
  }

  @Test
  public void should_support_mssql_with_jtds_driver() {
    Configuration config = Configuration.builder()
      .setProperty("sonar.jdbc.url", "jdbc:jtds:sqlserver://localhost/sonar")
      .build();

    DatabaseClient databaseClient = DatabaseFactory.create(config, mock(Locators.class));

    assertThat(databaseClient.getDialect()).isEqualTo("mssql");
  }

  @Test
  public void should_not_support_sybase() {
    thrown.expect(IllegalArgumentException.class);

    Configuration config = Configuration.builder()
      .setProperty("sonar.jdbc.url", "jdbc:sybase")
      .build();

    DatabaseFactory.create(config, mock(Locators.class));
  }

  /**
   * http://jira.sonarsource.com/browse/ORCH-54
   */
  @Test
  public void jdbcDriverLocatedInMavenRepository() {
    Locators locators = mock(Locators.class);
    File driverFile = getDriverFile();
    when(locators.locate(MavenLocation.create("oracle", "oracle", "10.1"))).thenReturn(driverFile);

    Configuration config = Configuration.builder()
        .setProperty("sonar.jdbc.url", "jdbc:oracle:thin:@localhost/XE")
        .setProperty("sonar.jdbc.driverMavenKey", "oracle:oracle:10.1")
        .build();

    DatabaseClient client = DatabaseFactory.create(config, locators);
    assertThat(client.getDriverFile()).isEqualTo(driverFile);
  }

  @Test
  public void jdbcDriverLocatedInMavenRepository_bad_format() {
    thrown.expect(IllegalArgumentException.class);
    Locators locators = mock(Locators.class);
    Configuration config = Configuration.builder()
        .setProperty("sonar.jdbc.driverMavenKey", "foo")
        .build();

    DatabaseFactory.create(config, locators);
  }

  private File getDriverFile() {
    return FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/db/DatabaseFactoryTest/driver.txt"));
  }
}
