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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultDatabaseTest {

  private static DefaultDatabase db;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void startDb() {
    db = new DefaultDatabase(H2.builder()
      .setUrl("jdbc:h2:mem:sonar;DB_CLOSE_DELAY=-1")
      .setDropAndCreate(false)
      .build());
    db.start();
    db.executeDdl(
      "CREATE TABLE \"METRICS\"( \"KEE\" VARCHAR(10), \"NAME\" VARCHAR(50))",
      "CREATE TABLE \"PROJECTS\"( \"KEE\" VARCHAR(10), \"NAME\" VARCHAR(50))",
      "CREATE TABLE \"PROPERTIES\"( \"ID\" DECIMAL, \"RESOURCE_ID\" DECIMAL)");
  }

  @Before
  public void insertData() {
    db.executeDdl(
      "DELETE FROM METRICS",
      "INSERT INTO METRICS(KEE,NAME) VALUES ('ncloc', 'Lines of Code')",
      "INSERT INTO METRICS(KEE,NAME) VALUES ('lines', 'Lines')",
      "DELETE FROM PROJECTS",
      "INSERT INTO PROJECTS(KEE,NAME) VALUES ('struts', 'Apache Struts')",
      "DELETE FROM PROPERTIES",
      "INSERT INTO PROPERTIES(ID, RESOURCE_ID) VALUES (1, 10)",
      "INSERT INTO PROPERTIES(ID, RESOURCE_ID) VALUES (2, NULL)");
  }

  @AfterClass
  public static void stopDb() {
    if (db != null) {
      db.stop();
      db = null;
    }
  }

  @Test
  public void test_drop_and_create() throws IOException {
    File h2Folder = temporaryFolder.newFolder();
    String url = "jdbc:h2:file:" + h2Folder.getAbsolutePath() + "/test;DB_CLOSE_DELAY=-1";
    // create DB with some tables
    DefaultDatabase db = null;
    try {
      db = new DefaultDatabase(H2.builder()
        .setUrl(url)
        .setLogin("")
        .setPassword("")
        .setRootUrl(url)
        .setRootLogin("")
        .setRootPassword("")
        .setDropAndCreate(false)
        .build());
      db.start();
      db.executeDdl(
          "CREATE TABLE \"METRICS\"( \"KEE\" VARCHAR(10), \"NAME\" VARCHAR(50))",
          "CREATE TABLE \"PROJECTS\"( \"KEE\" VARCHAR(10), \"NAME\" VARCHAR(50))",
          "CREATE TABLE \"PROPERTIES\"( \"ID\" DECIMAL, \"RESOURCE_ID\" DECIMAL)");

      List<Map<String, String>> maps = db.executeSql("SHOW TABLES");
      assertThat(maps).hasSize(3);
    } finally {
      if (db != null) {
        db.stop();
      }
    }

    testCreatingConcurrentDb(url);
  }

  private void testCreatingConcurrentDb(String url) {
    // testing killOtherConnections, it won't succeed on H2 because we are not running in multithreaded mode
    // but the method will not hang and return without error
    DefaultDatabase db = null;
    try {
      db = new DefaultDatabase(H2.builder()
          .setUrl(url)
          .setLogin("")
          .setPassword("")
          .setRootUrl(url)
          .setRootLogin("")
          .setRootPassword("")
          .setDropAndCreate(false)
          .build());

      db.start();
      db.killOtherConnections();
    } finally {
      if (db != null) {
        db.stop();
      }
    }

    // testing dropAndCreate option
    db = null;
    try {
      db = new DefaultDatabase(H2.builder()
        .setUrl(url)
        .setLogin("")
        .setPassword("")
        .setRootUrl(url)
        .setRootLogin("")
        .setRootPassword("")
        .setDropAndCreate(true)
        .build());

      db.start();
      List<Map<String, String>> maps = db.executeSql("SHOW TABLES");
      assertThat(maps).isEmpty();
    } finally {
      if (db != null) {
        db.stop();
      }
    }
  }

  @Test
  public void drop_and_create_throws_ISE_if_root_password_is_incorrect() throws IOException {
    File h2Folder = temporaryFolder.newFolder();
    String url = "jdbc:h2:file:" + h2Folder.getAbsolutePath() + "/test;DB_CLOSE_DELAY=-1";
    DefaultDatabase db = null;
    try {
      db = new DefaultDatabase(H2.builder()
        .setUrl(url)
        .setLogin("")
        .setPassword("")
        .setRootUrl(url)
        .setRootLogin("foo")
        .setRootPassword("")
        .setDropAndCreate(true)
        .build());

      db.start();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail to dropAndCreate database");
    } finally {
      if (db != null) {
        db.stop();
      }
    }
  }

  @Test
  public void shouldCreateDatabase() throws SQLException {
    assertConnected(db);
  }

  @Test
  public void openAndCloseConnection() throws SQLException {
    Connection connection = db.openConnection();
    assertThat(connection).isNotNull();
    assertThat(connection.isClosed()).isFalse();

    db.closeQuietly(connection);
    assertThat(connection.isClosed()).isTrue();
  }

  @Test
  public void failIfUnknownDriver() {
    thrown.expect(RuntimeException.class);

    DefaultDatabase database = new DefaultDatabase(H2.builder().setDriverClassName("not.a.driver.Class").build());
    database.start();
  }

  @Test
  public void countSql() {
    assertThat(db.countSql("select count(kee) from metrics")).isEqualTo(2);
  }

  @Test
  public void countSql_fail_if_bad_sql() {
    thrown.expect(RuntimeException.class);
    db.countSql("this is not sql");
  }

  @Test
  public void executeSql() {
    List<Map<String, String>> rows = db.executeSql("select * from metrics order by kee");

    assertThat(rows.size()).isEqualTo(2);
    assertThat(rows.get(0).get("KEE")).isEqualTo("lines");
    assertThat(rows.get(0).get("NAME")).isEqualTo("Lines");
    assertThat(rows.get(1).get("KEE")).isEqualTo("ncloc");
    assertThat(rows.get(1).get("NAME")).isEqualTo("Lines of Code");
  }

  @Test
  public void executeSql_empty_result() {
    List<Map<String, String>> rows = db.executeSql("select * from metrics where kee='xxx'");
    assertThat(rows.size()).isEqualTo(0);
  }

  @Test
  public void executeSql_fail_if_bad_sql() {
    thrown.expect(RuntimeException.class);
    db.executeSql("this is not sql");
  }

  @Test
  public void truncate() {
    assertThat(db.countSql("select count(kee) from metrics")).isGreaterThan(0);
    db.truncate("metrics");
    assertThat(db.countSql("select count(kee) from metrics")).isEqualTo(0);
  }

  @Test
  public void truncateShouldFailIfUppercase() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Table name [METRICS] should be lowercase to avoid issues");
    db.truncate("METRICS");
  }

  private void assertConnected(Database operations) throws SQLException {
    Connection connection = operations.openConnection();
    try {
      assertThat(connection.isClosed()).isFalse();

    } finally {
      connection.close();
    }
  }
}
