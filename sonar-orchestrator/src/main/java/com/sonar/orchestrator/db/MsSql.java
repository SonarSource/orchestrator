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

public final class MsSql extends DatabaseClient {

  private MsSql(Builder builder) {
    super(builder);
  }

  static MsSqlBuilder jtdsBuilder() {
    return (MsSqlBuilder)new MsSqlBuilder()
      // set default values
      .setDriverClassName("net.sourceforge.jtds.jdbc.Driver")
      .setUrl("jdbc:jtds:sqlserver://localhost/sonar;SelectMethod=Cursor")
      .setRootUrl("jdbc:jtds:sqlserver://localhost")
      .setRootLogin("sa")
      .setRootPassword("");
  }

  static MsSqlBuilder msBuilder() {
    return (MsSqlBuilder)new MsSqlBuilder()
      // set default values
      .setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
      .setUrl("jdbc:sqlserver://localhost;databaseName=sonar")
      .setRootUrl("jdbc:sqlserver://localhost")
      .setRootLogin("sa")
      .setRootPassword("");
  }

  public static final class MsSqlBuilder extends Builder<MsSql> {
    private MsSqlBuilder() {
    }

    @Override
    public MsSql build() {
      return new MsSql(this);
    }
  }

  @Override
  public String getDialect() {
    return "mssql";
  }

  @Override
  public String[] getDropDdl() {
    return new String[] {
      "drop database [" + getLogin() + "]",
      "drop login [" + getLogin() + "]"};
  }

  @Override
  public String[] getCreateDdl() {
    return new String[] {
      // case-sensitive and accent-sensitive collation
      "create database [" + getLogin() + "] collate SQL_Latin1_General_CP1_CS_AS",
      "CREATE LOGIN [" + getLogin() + "] WITH PASSWORD = '" + getPassword() + "', CHECK_POLICY=OFF, DEFAULT_DATABASE=[" + getLogin() + "]",
      "USE [" + getLogin() + "]",
      "sp_adduser N'" + getLogin() + "', N'" + getLogin() + "'",
      "EXEC sp_addrolemember N'db_owner', N'" + getLogin() + "'"
    };
  }

  @Override
  public String getSelectConnectionIdsSql() {
    return "SELECT spid as id FROM sys.sysprocesses "
      + " WHERE dbid > 0 and loginame = '" + getLogin() + "'";

  }

  @Override
  public String getKillConnectionSql(String processID) {
    return "KILL " + processID;
  }

}
