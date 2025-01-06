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

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface Database {

  DatabaseClient getClient();

  /**
   * Truncate a table. It does not fail if the table does not exist.
   */
  Database truncate(String tableName);

  /**
   * Example : countSql("count(kee) from metrics")
   */
  int countSql(String sql);

  /**
   * Execute SQL request and return a list of rows. A row is a map of column name (in upper case) to string value.
   */
  List<Map<String, String>> executeSql(String sql);

  Connection openConnection();

  /**
   * clean other sql connection
   */
  void killOtherConnections();

  /**
   * Close the JDBC connection. Failures are logged with WARN level then are ignored.
   */
  Database closeQuietly(Connection connection);

  Map<String, String> getSonarProperties();

}
