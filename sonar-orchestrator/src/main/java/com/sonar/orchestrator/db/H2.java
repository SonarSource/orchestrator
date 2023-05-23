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

import java.io.File;
import java.net.InetAddress;
import org.apache.commons.lang.StringUtils;

import static com.sonar.orchestrator.util.NetworkUtils.getNextAvailablePort;
import static java.lang.String.format;

public final class H2 extends DatabaseClient {

  private H2(Builder builder) {
    super(builder);
  }

  @Override
  public String[] getDropDdl() {
    return new String[]{ "DROP ALL OBJECTS" };
  }

  @Override
  public String getSelectConnectionIdsSql() {
    if (super.getDBMajorVersion() == 1) {
      return "select id from information_schema.sessions where user_name <> '" + getRootLogin() + "'";
    } else {
      return "select session_id from information_schema.sessions where user_name <> '" + getRootLogin() + "'";
    }
  }

  @Override
  public String getKillConnectionSql(String connectionId) {
    return "CALL CANCEL_SESSION(" + connectionId + ")";
  }

  public static H2Builder builder() {
    return new H2Builder();
  }

  public static final class H2Builder extends Builder<H2> {
    private H2Builder() {
      // set default values
      setDropAndCreate(false);
      setDriverClassName("org.h2.Driver");

      InetAddress address = InetAddress.getLoopbackAddress();
      int port = getNextAvailablePort(address);
      setUrl(format("jdbc:h2:tcp://%s:%d/sonar;USER=sonar;PASSWORD=sonar", address.getHostAddress(), port));
    }

    @Override
    public Builder<H2> setSchema(String schema) {
      // Can not change schema on H2
      return this;
    }

    @Override
    public Builder<H2> setDriverFile(File driverFile) {
      // Can not change driver on H2
      return this;
    }

    @Override
    public Builder<H2> setUrl(String s) {
      String path = StringUtils.substringAfterLast(s, ":");
      String port = StringUtils.substringBefore(path, "/");
      addProperty("sonar.embeddedDatabase.port", port);
      return super.setUrl(s);
    }

    @Override
    public H2 build() {
      return new H2(this);
    }
  }

  @Override
  public String getDialect() {
    return "h2";
  }
}
