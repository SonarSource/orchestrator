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

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;

public final class DatabaseFactory {

  private DatabaseFactory() {
    // only static methods
  }

  public static DatabaseClient create(Configuration config, Locators locators) {
    String url = config.getString("sonar.jdbc.url");
    DatabaseClient.Builder builder = newBuilderForUrl(url);

    String s = config.getString("sonar.jdbc.schema");
    if (!isEmpty(s)) {
      builder.setSchema(s);
    }
    s = config.getString("sonar.jdbc.username");
    if (!isEmpty(s)) {
      builder.setLogin(s);
    }
    s = config.getString("sonar.jdbc.password");
    if (!isEmpty(s)) {
      builder.setPassword(s);
    }
    s = config.getString("sonar.jdbc.rootUrl");
    if (!isEmpty(s)) {
      builder.setRootUrl(s);
    }
    s = config.getString("sonar.jdbc.rootUsername");
    if (!isEmpty(s)) {
      builder.setRootLogin(s);
    }
    s = config.getString("sonar.jdbc.rootPassword");
    if (!isEmpty(s)) {
      builder.setRootPassword(s);
    }
    s = config.getString("sonar.jdbc.driverFile");
    if (!isEmpty(s)) {
      builder.setDriverFile(new File(s));
    }
    s = config.getString("sonar.jdbc.driverMavenKey");
    if (!isEmpty(s)) {
      feedDriverMavenKey(locators, builder, s);
    }

    String value = config.getString("orchestrator.keepDatabase", "false");
    boolean keepDatabase = !isEmpty(value) ? Boolean.valueOf(value) : false;
    if (keepDatabase) {
      builder.setDropAndCreate(false);
    }

    return builder.build();
  }

  private static void feedDriverMavenKey(Locators locators, DatabaseClient.Builder builder, String propertyValue) {
    String[] fields = propertyValue.split(":");
    checkArgument(fields.length == 3, "Format is groupId:artifactId:version. Please check the property sonar.jdbc.driverMavenKey: %s", propertyValue);
    MavenLocation location = MavenLocation.create(fields[0], fields[1], fields[2]);
    File file = locators.locate(location);
    checkState(file.exists(), "Driver file does not exist: %s", location);
    builder.setDriverFile(file);
  }

  private static DatabaseClient.Builder newBuilderForUrl(String url) {
    DatabaseClient.Builder builder;
    if (isEmpty(url) || url.startsWith("jdbc:h2:")) {
      builder = H2.builder();
    } else if (url.startsWith("jdbc:oracle:")) {
      builder = Oracle.builder();
    } else if (url.startsWith("jdbc:mysql:")) {
      builder = MySql.builder();
    } else if (url.startsWith("jdbc:jtds:")) {
      builder = MsSql.jtdsBuilder();
    } else if (url.startsWith("jdbc:sqlserver:")) {
      builder = MsSql.msBuilder();
    } else if (url.startsWith("jdbc:postgresql:")) {
      builder = PostgreSql.builder();
    } else {
      throw new IllegalArgumentException("Unsupported DB: " + url);
    }
    if (!isEmpty(url)) {
      builder.setUrl(url);
    }
    return builder;
  }
}
