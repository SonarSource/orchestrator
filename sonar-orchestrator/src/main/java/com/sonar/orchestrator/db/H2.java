/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.sonar.orchestrator.util.NetworkUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;

public final class H2 extends DatabaseClient {

  private H2(Builder builder) {
    super(builder);
  }

  public static H2 create() {
    return builder().build();
  }

  public static H2Builder builder() {
    return new H2Builder();
  }

  public static final class H2Builder extends Builder<H2> {
    private H2Builder() {
      // set default values
      setDropAndCreate(false);
      setDriverClassName("org.h2.Driver");
      int port = NetworkUtils.getNextAvailablePort();
      setUrl("jdbc:h2:tcp://localhost:" + port + "/sonar;USER=sonar;PASSWORD=sonar");
    }

    @Override
    public Builder<H2> setSchema(String schema) {
      // Can not change schema on H2
      return this;
    }

    @Override
    public Builder<H2> setUrl(String url) {
      String port = StringUtils.substringBetween(url, "localhost:", "/");
      addProperty("sonar.embeddedDatabase.port", port);
      return super.setUrl(url);
    }

    @Override
    public Builder<H2> setDriverFile(File driverFile) {
      // Can not change driver on H2
      return this;
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
