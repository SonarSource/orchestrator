/*
 * Orchestrator
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.orchestrator.db;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import javax.annotation.CheckForNull;


import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public abstract class DatabaseClient {

  private boolean dropAndCreate;
  private String driverClassName;
  private File driverFile;
  private String url;
  private String schema;
  private String login;
  private String password;
  private String rootUrl;
  private String rootLogin;
  private String rootPassword;
  private Map<String, String> additionalProperties;
  // jdbc metadata
  private int dbMajorVersion;
  private int dbMinorVersion;
  private String dbProductName;

  protected DatabaseClient(Builder builder) {
    dropAndCreate = builder.dropAndCreate;
    driverClassName = builder.driverClassName;
    driverFile = builder.driverFile;
    url = builder.url;
    schema = builder.schema;
    login = builder.login;
    password = builder.password;
    rootLogin = builder.rootLogin;
    rootUrl = builder.rootUrl;
    rootPassword = builder.rootPassword;
    additionalProperties = builder.additionalProperties;
  }

  public abstract String getDialect();

  public String getDriverClassName() {
    return driverClassName;
  }

  public File getDriverFile() {
    return driverFile;
  }

  public boolean isDropAndCreate() {
    return dropAndCreate;
  }

  public void setDropAndCreate(boolean dropAndCreate) {
    this.dropAndCreate = dropAndCreate;
  }

  public String getUrl() {
    return url;
  }

  public String getSchema() {
    return schema;
  }

  public String getLogin() {
    return login;
  }

  public String getPassword() {
    return password;
  }

  public String getRootUrl() {
    return rootUrl;
  }

  public String getRootLogin() {
    return rootLogin;
  }

  public String getRootPassword() {
    return rootPassword;
  }

  public Map<String, String> getAdditionalProperties() {
    return additionalProperties;
  }

  public int getDBMajorVersion( )  {
    return dbMajorVersion;
  }
  public int getDBMinorVersion( )  {
    return dbMinorVersion;
  }
  public String getDBProductName( )  {
    return dbProductName;
  }

  public String[] getDropDdl() {
    // to be overridden
    return new String[0];
  }

  public String[] getCreateDdl() {
    // to be overridden
    return new String[0];
  }
  
  //  list all the SQL connections ID not own by getlogin()
  @CheckForNull
  public String getSelectConnectionIdsSql() {
    // to be overridden
    return null;
  }

  // returns string line, what to kill one single process
  // process is designated with a String, coz' the different databases we used have different ways to designate
  @CheckForNull
  public String getKillConnectionSql(String connectionId) {
    // to be overridden
    return null;
  }

  public final Map<String, String> getProperties() {
    Map<String, String> props = Maps.newHashMap();
    props.putAll(additionalProperties);
    addProperty(props, "sonar.jdbc.dialect", getDialect());
    addProperty(props, "sonar.jdbc.url", url);
    addProperty(props, "sonar.jdbc.schema", schema);
    addProperty(props, "sonar.jdbc.username", login);
    addProperty(props, "sonar.jdbc.password", password);
    addProperty(props, "sonar.jdbc.rootUrl", rootUrl);
    addProperty(props, "sonar.jdbc.rootUsername", rootLogin);
    addProperty(props, "sonar.jdbc.rootPassword", rootPassword);
    addProperty(props, "sonar.jdbc.driverClassName", driverClassName);
    if (driverFile != null) {
      addProperty(props, "sonar.jdbc.driverFile", driverFile.getAbsolutePath());
    }
    return props;
  }

  private static void addProperty(Map<String, String> props, String key, String value) {
    if (value != null) {
      props.put(key, value);
    }
  }

  @Override
  public String toString() {
    return getProperties().toString() + ";" + super.toString();

  }

  public Connection openConnection(  ) throws SQLException {
    Connection conn = DriverManager.getConnection(getUrl(), getLogin(), getPassword());

    fillDbMetadata(conn);

    return conn;
  }

  public Connection openRootConnection( ) throws SQLException {
    Connection conn = DriverManager.getConnection(getRootUrl(), getRootLogin( ), getRootPassword( ));
    fillDbMetadata(conn);
    return conn;
  }

  private void fillDbMetadata(Connection conn) {
    try {
      DatabaseMetaData meta = conn.getMetaData();
      dbMajorVersion = meta.getDatabaseMajorVersion();
      dbMinorVersion = meta.getDatabaseMinorVersion();
      dbProductName = meta.getDatabaseProductName();
    } catch( SQLException e ) {
      throw new IllegalStateException("Can't get JDBC metadata", e);
    }
  }
  

  public abstract static class Builder<D extends DatabaseClient> {
    private boolean dropAndCreate = true;
    private String driverClassName;
    private File driverFile;
    private String url;
    private String schema;
    private String login = "sonar";
    private String password = "sonar";
    private String rootUrl;
    private String rootLogin;
    private String rootPassword;
    private Map<String, String> additionalProperties = Maps.newHashMap();

    protected Builder() {
    }

    public boolean isDropAndCreate() {
      return dropAndCreate;
    }

    public Builder<D> setDropAndCreate(boolean b) {
      this.dropAndCreate = b;
      return this;
    }

    public String getDriverClassName() {
      return driverClassName;
    }

    public Builder<D> setDriverClassName(String driverClassName) {
      this.driverClassName = driverClassName;
      return this;
    }

    public File getDriverFile() {
      return driverFile;
    }

    public Builder<D> setDriverFile(File driverFile) {
      Preconditions.checkNotNull(driverFile);
      Preconditions.checkArgument(driverFile.exists(), "Driver file does not exist: " + driverFile);
      Preconditions.checkArgument(driverFile.isFile(), "Driver is not a file");
      this.driverFile = driverFile;
      return this;
    }

    public String getUrl() {
      return url;
    }

    public Builder<D> setUrl(String url) {
      this.url = url;
      return this;
    }

    public String getSchema() {
      return schema;
    }

    public Builder<D> setSchema(String s) {
      this.schema = s;
      return this;
    }

    public String getLogin() {
      return login;
    }

    public Builder<D> setLogin(String login) {
      this.login = login;
      return this;
    }

    public String getPassword() {
      return password;
    }

    public Builder<D> setPassword(String password) {
      this.password = password;
      return this;
    }

    public String getRootUrl() {
      return rootUrl;
    }

    public Builder<D> setRootUrl(String rootUrl) {
      this.rootUrl = rootUrl;
      return this;
    }

    public String getRootLogin() {
      return rootLogin;
    }

    public Builder<D> setRootLogin(String rootLogin) {
      this.rootLogin = rootLogin;
      return this;
    }

    public String getRootPassword() {
      return rootPassword;
    }

    public Builder<D> setRootPassword(String rootPassword) {
      this.rootPassword = rootPassword;
      return this;
    }

    public Builder<D> addProperty(String key, String value) {
      additionalProperties.put(key, value);
      return this;
    }

    public abstract D build();


  }
}
