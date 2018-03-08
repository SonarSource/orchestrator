/*
 * Orchestrator
 * Copyright (C) 2011-2018 SonarSource SA
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

public final class PostgreSql extends DatabaseClient {

  private PostgreSql(Builder builder) {
    super(builder);
  }

  public static PostgreSqlBuilder builder() {
    return new PostgreSqlBuilder();
  }

  public static final class PostgreSqlBuilder extends Builder<PostgreSql> {
    private PostgreSqlBuilder() {
      // set default values
      setDriverClassName("org.postgresql.Driver");
      setUrl("jdbc:postgresql://localhost/sonar");
      setRootUrl("jdbc:postgresql://localhost");
      setRootLogin("postgres");
      setRootPassword("postgres");
    }

    @Override
    public PostgreSql build() {
      return new PostgreSql(this);
    }
  }

  @Override
  public String getDialect() {
    return "postgresql";
  }

  @Override
  public String[] getDropDdl() {
    return new String[] {"drop database IF EXISTS \"" + getLogin() + "\"",
      String.format("drop user if exists \"%s\"", getLogin())};
  }

  @Override
  public String[] getCreateDdl() {
    return new String[] {
      "create database \"" + getLogin() + "\"",
      String.format("CREATE USER \"%s\" WITH PASSWORD '%s' CREATEDB",
        getLogin(), getPassword())};
  }

  @Override
  public String getSelectConnectionIdsSql() {
    if(getDBMajorVersion()==8) {
      return "SELECT procpid as id FROM pg_stat_activity WHERE usename = '" + getLogin() + "'";
    } else {
      return "SELECT pid as id FROM pg_stat_activity WHERE usename = '" + getLogin() + "'";
    }
  }

  @Override
  public String getKillConnectionSql(String processID) {
    return "SELECT pg_terminate_backend( " + processID + ");";

  }

}
