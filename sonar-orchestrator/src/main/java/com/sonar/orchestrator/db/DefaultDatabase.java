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

import com.sonar.orchestrator.config.Configuration;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;

public final class DefaultDatabase implements Database {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultDatabase.class);

  private DatabaseClient databaseClient;
  private boolean started = false;

  public DefaultDatabase(Configuration config) {
    this.databaseClient = DatabaseFactory.create(config, config.locators());
  }

  public DefaultDatabase(DatabaseClient client) {
    this.databaseClient = client;
  }

  public void start() {
    if (!started) {
      registerDriver();
      if (databaseClient.isDropAndCreate()) {
        dropAndCreateDatabase();
      }
      started = true;
    }
  }

  public void stop() {
    if (started) {
      started = false;
    }
  }

  @Override
  public Map<String, String> getSonarProperties() {
    return databaseClient.getProperties();
  }

  @Override
  public DatabaseClient getClient() {
    return databaseClient;
  }

  @Override
  public Database truncate(String tableName) {
    Connection connection = openConnection();
    try {
      truncate(tableName, connection);
      return this;

    } finally {
      closeQuietly(connection);
    }
  }

  @Override
  public int countSql(String sql) {
    LOG.info("Count sql");
    Connection connection = openConnection();
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = connection.createStatement();
      LOG.debug("Execute: {}", sql);
      rs = statement.executeQuery(sql);
      rs.next();
      return rs.getInt(1);

    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute SQL: " + sql, e);

    } finally {
      closeQuietly(rs);
      closeQuietly(statement);
      closeQuietly(connection);
    }
  }

  @Override
  public List<Map<String, String>> executeSql(String sql) {
    Connection connection = openConnection();
    try {
      return executeSql(connection, sql);
    } catch (Exception e) {
      throw new IllegalArgumentException("Fail to execute SQL request: " + sql, e);

    } finally {
      closeQuietly(connection);
    }
  }

  private List<Map<String, String>> executeSql(Connection connection, String sql) {
    List<Map<String, String>> list = new ArrayList<>();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery(sql);

      ResultSetMetaData rsmd = rs.getMetaData();
      while (rs.next()) {
        Map<String, String> row = new HashMap<>();
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
          String columnName = rsmd.getColumnName(i).toUpperCase(Locale.ENGLISH);
          row.put(columnName, rs.getString(i));
        }
        list.add(row);
      }
      return list;

    } catch (Exception e) {
      throw new IllegalArgumentException("Fail to execute SQL request: " + sql, e);

    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
    }
  }

  @Override
  public Connection openConnection() {
    if (started) {
      try {
        LOG.info("Open connection");
        return databaseClient.openConnection();

      } catch (SQLException e) {
        throw new IllegalStateException("Fail to open a JDBC connection", e);
      }
    }
    throw new IllegalStateException("Can not open a JDBC connection as long as the database is not started");
  }

  private DefaultDatabase truncate(String tableName, Connection connection) {
    if (!tableName.matches("[\\p{javaLowerCase}_]*")) {
      // ORCH-172
      throw new IllegalStateException("Table name [" + tableName + "] should be lowercase to avoid issues");
    }
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("TRUNCATE TABLE " + tableName);
      // commit is useless on some databases
      connection.commit();
    } catch (SQLException e) {
      // frequent use-case : the table does not exist
      LOG.warn("Truncation of tables failed", e);
    }
    return this;
  }

  @Override
  public DefaultDatabase closeQuietly(@Nullable Connection connection) {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      LOG.warn("Fail to close JDBC connection", e);
    }
    return this;
  }

  private DefaultDatabase closeQuietly(@Nullable ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (Exception e) {
        LOG.warn("Fail to close result set", e);
      }
    }
    return this;
  }

  private DefaultDatabase closeQuietly(@Nullable Statement stmt) {
    if (stmt != null) {
      try {
        stmt.close();
      } catch (Exception e) {
        LOG.warn("Fail to close statement", e);
      }
    }
    return this;
  }

  private DefaultDatabase dropAndCreateDatabase() {
    Connection connection = null;
    try {
      // get a connection as root, to be allowed to kill the other connections
      connection = databaseClient.openRootConnection();
      killOtherConnections(connection);
      dropDatabase(connection);
      createDatabase(connection);

    } catch (SQLException e) {
      throw new IllegalStateException("Fail to dropAndCreate database", e);

    } catch (InterruptedException e) {
      throw new IllegalStateException("Interrupted dropAndCreate database", e);

    } finally {
      closeQuietly(connection);
    }
    return this;
  }

  @Override
  public void killOtherConnections() {
    Connection connection = null;
    try {
      // get a connection as root, to be be allowed to kill the other connections
      LOG.info("Open root connection");
      connection = databaseClient.openRootConnection();
      killOtherConnections(connection);

    } catch (SQLException e) {
      throw new IllegalStateException("Fail to clean up the other connections", e);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Killing of connections got interrupted", e);
    } finally {
      closeQuietly(connection);
    }
  }

  /**
   * kills all the user connections, but myself
   * requires elevated privilege, (rootLogin)
   * some of the databases requires some delay to kill the connections, let's give several tries
   */
  void killOtherConnections(Connection connection) throws SQLException, InterruptedException {

    final long kNbAttempts = 3;
    final long kSecBetweenKillFactor = 5;

    long attemptToGo = kNbAttempts;
    List<String> spids = selectOtherConnections(connection);
    while (attemptToGo > 0 && !spids.isEmpty()) {
      // will make sleep 5 at loop 1, 10 at loop 2
      TimeUnit.SECONDS.sleep(kSecBetweenKillFactor * (kNbAttempts - attemptToGo));

      for (String spid : spids) {
        try (Statement stmt = connection.createStatement()) {
          String sql = databaseClient.getKillConnectionSql(spid);
          LOG.warn("Kill JDBC orphan {}", sql);
          stmt.execute(sql);
          // commit is useless on some databases
          if (!connection.getAutoCommit()) {
            connection.commit();
          }
        } catch (SQLException e) {
          LOG.error("Issue while killing connection", e);
        }
      }

      // prepare next loop
      spids = selectOtherConnections(connection);
      if (!spids.isEmpty()) {
        attemptToGo--;
        LOG.warn("Killing of orphan requires additional attempt {}", spids);
      } else {
        // we are finished.
        break;
      }
    }

    // final check
    if (!spids.isEmpty()) {
      throw new SQLException("Some connections remains dangling after killing " + spids.toString());
    }
  }

  private List<String> selectOtherConnections(Connection connection) throws SQLException {
    LOG.info("Query of opened connection");
    String sql = databaseClient.getSelectConnectionIdsSql();
    LOG.debug("Execute: {}", sql);
    if (sql == null) {
      return Collections.emptyList();
    }
    List<String> spids = new ArrayList<>();
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        String spid = rs.getString(1);
        if (!isEmpty(spid)) {
          spids.add(spid);
        }
      }
    }
    return spids;
  }

  private DefaultDatabase dropDatabase(Connection connection) {
    LOG.info("Drop database");
    for (String ddl : databaseClient.getDropDdl()) {
      try {
        executeDdl(connection, ddl);
      } catch (Exception e) {
        LOG.warn("Error while dropping the database, but this may be only noise: ", e);
      }
    }
    return this;
  }

  private DefaultDatabase createDatabase(Connection connection) {
    LOG.info("Create database");
    executeDdl(connection, databaseClient.getCreateDdl());
    executeDdlByRoot(databaseClient.getPermissionOnSchema());
    return this;
  }

  /**
   * We need to open a new connection, as root, on the non-root database, because the GRANT XXX ON SCHEMA instruction apply
   * to the database we are connected to, so we can't to it from the root database (otherwise that would grant permission on the root schema)
   */
  private void executeDdlByRoot(String... ddls) {
    Connection connectionNotRoot = null;
    try {
      // get a connection as root, but on the non-root database
      connectionNotRoot = databaseClient.openRootConnectionOnNonRootDatabase();
      executeDdl(connectionNotRoot, ddls);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to dropAndCreate database", e);
    } finally {
      closeQuietly(connectionNotRoot);
    }
  }

  DefaultDatabase executeDdl(String... ddls) {
    Connection connection = openConnection();
    try {
      executeDdl(connection, ddls);
    } finally {
      closeQuietly(connection);
    }
    return this;
  }

  DefaultDatabase executeDdl(Connection connection, String... ddls) {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      for (String ddl : ddls) {
        LOG.debug("Execute: {}", ddl);
        stmt.executeUpdate(ddl);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute ddl", e);

    } finally {
      closeQuietly(stmt);
    }
    return this;
  }

  private void registerDriver() {
    try {
      LOG.info("Register JDBC driver: {}", databaseClient.getDriverClassName());
      LOG.debug("Connection data: {}", databaseClient);
      DriverManager.registerDriver((Driver) Class.forName(databaseClient.getDriverClassName()).newInstance());

    } catch (Exception e) {
      throw new IllegalStateException("Fail to load JDBC driver: " + databaseClient.getDriverClassName(), e);
    }
  }
}
