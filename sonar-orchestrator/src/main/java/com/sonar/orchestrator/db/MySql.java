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

public final class MySql extends DatabaseClient {

  private MySql(Builder builder) {
    super(builder);
  }

  public static MySqlBuilder builder() {
    return new MySqlBuilder();
  }

  public static final class MySqlBuilder extends Builder<MySql> {
    private MySqlBuilder() {
      // set default values
      setDriverClassName("com.mysql.jdbc.Driver");
      setUrl("jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8");
      setRootUrl("jdbc:mysql://localhost:3306");
      setRootLogin("root");
      setRootPassword("");
      setSchema("sonar");
    }

    @Override
    public MySql build() {
      return new MySql(this);
    }
  }

  @Override
  public String getDialect() {
    return "mysql";
  }

  @Override
  public String[] getDropDdl() {
    return new String[] {
      "drop database IF EXISTS `" + getSchema() + "`",
      "drop user `" + getLogin() + "`@`%`"};
  }

  @Override
  public String[] getCreateDdl() {
    return new String[] {
      "create user `" + getLogin() + "` IDENTIFIED BY '" + getPassword() + "'",
      String.format("create database `%s`", getSchema()),
      String.format("GRANT ALL ON `%s`.* TO `%s`@`%%` IDENTIFIED BY '%s'", getSchema(), getLogin(), getPassword()),
      String.format("GRANT ALL ON `%s`.* TO `%s`@`localhost` IDENTIFIED BY '%s'", getSchema(), getLogin(), getPassword()),
    };
  }

  @Override
  public String getSelectConnectionIdsSql() {
    return "SELECT ID FROM information_schema.PROCESSLIST WHERE USER = '" + getLogin() + "'";
  }

  @Override
  public String getKillConnectionSql(String processID) {
    return "KILL " + processID;

  }
}
