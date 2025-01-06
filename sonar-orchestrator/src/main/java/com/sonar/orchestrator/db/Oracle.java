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

public final class Oracle extends DatabaseClient {

  private Oracle(Builder builder) {
    super(builder);
  }

  public static OracleBuilder builder() {
    return new OracleBuilder();
  }

  public static final class OracleBuilder extends Builder<Oracle> {
    private OracleBuilder() {
      setDriverClassName("oracle.jdbc.OracleDriver");
      setUrl("jdbc:oracle:thin:@localhost/XE");
      setRootUrl("jdbc:oracle:thin:@localhost/XE");
      setRootLogin("SYSTEM");
      setRootPassword("password");
    }

    @Override
    public Oracle build() {
      if (getDriverFile() == null) {
        throw new IllegalArgumentException("Path to Oracle JDBC Driver is missing. Please set the property 'sonar.jdbc.driverFile'.");
      }
      return new Oracle(this);
    }
  }

  @Override
  public String getDialect() {
    return "oracle";
  }

  @Override
  public String[] getDropDdl() {
    return new String[]{
            "BEGIN EXECUTE IMMEDIATE (' DROP USER " + getLogin() + " CASCADE '); END;"
    };
  }

  @Override
  public String[] getCreateDdl() {
    return new String[]{
      "CREATE USER " + getLogin() + " IDENTIFIED BY " + getPassword() + " DEFAULT TABLESPACE USERS ACCOUNT UNLOCK",
      "GRANT UNLIMITED TABLESPACE TO " + getLogin(),
      "GRANT CONNECT TO " + getLogin(),
      "GRANT RESOURCE TO " + getLogin(),
      "GRANT CREATE TABLE to " + getLogin(),
      "GRANT CREATE SEQUENCE to " + getLogin()};
  }

  @Override
  public String getSelectConnectionIdsSql() {
    return "SELECT SID || ',' || SERIAL# AS ID  FROM sys.v_$session WHERE USERNAME = '" + getLogin().toUpperCase() + "'";
  }

  @Override
  public String getKillConnectionSql(String processID) {
    return "ALTER SYSTEM DISCONNECT SESSION '" + processID + "' IMMEDIATE";

  }
  
}
