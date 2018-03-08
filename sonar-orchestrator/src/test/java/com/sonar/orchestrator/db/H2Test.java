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

import java.net.InetAddress;
import org.junit.Test;

import java.io.File;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class H2Test {

  @Test
  public void test_default_build() {
    H2 h2 = H2.builder().build();

    assertThat(h2.getDialect()).isEqualTo("h2");
    assertThat(h2.getDriverClassName()).isEqualTo("org.h2.Driver");
    assertThat(h2.getUrl()).matches(format("jdbc:h2:tcp://%s:[0-9]+/sonar;USER=sonar;PASSWORD=sonar", InetAddress.getLoopbackAddress().getHostAddress()));
    assertThat(h2.getLogin()).isEqualTo("sonar");
    assertThat(h2.getPassword()).isEqualTo("sonar");
    assertThat(h2.isDropAndCreate()).isFalse();
  }

  @Test
  public void createDdl_is_not_defined() {
    H2 h2 = H2.builder().build();

    assertThat(h2.getCreateDdl()).isEmpty();
  }

  @Test
  public void dropDdl_is_defined() {
    H2 h2 = H2.builder().build();

    assertThat(h2.getDropDdl()).containsOnly("DROP ALL OBJECTS");
  }

  @Test
  public void getSelectConnectionIdsSql_is_defined_and_excludes_root_login() {
    H2 h2 = H2.builder().setLogin("foo").setRootLogin("bar").build();

    assertThat(h2.getSelectConnectionIdsSql()).isEqualTo("select id from information_schema.sessions where user_name <> 'bar'");
  }

  @Test
  public void getKillConnectionSql_is_defined() {
    H2 h2 = H2.builder().build();

    assertThat(h2.getKillConnectionSql("123")).isEqualTo("CALL CANCEL_SESSION(123)");

  }

  @Test
  public void shouldSetPortProperty() {
    H2 h2 = H2.builder().build();

    assertThat(h2.getUrl()).matches("jdbc:h2:tcp://.*:\\d*/sonar;USER=sonar;PASSWORD=sonar");
    assertThat(h2.getAdditionalProperties().get("sonar.embeddedDatabase.port")).isNotEmpty();
  }

  @Test
  public void shouldSetPortPropertyWhenChangingUrl() {
    H2 h2 = H2.builder().setUrl("jdbc:h2:tcp://localhost:1234/sonar;USER=sonar;PASSWORD=sonar").build();

    assertThat(h2.getUrl()).isEqualTo("jdbc:h2:tcp://localhost:1234/sonar;USER=sonar;PASSWORD=sonar");
    assertThat(h2.getAdditionalProperties().get("sonar.embeddedDatabase.port")).isEqualTo("1234");
  }

  @Test
  public void driver_and_schema_must_not_be_set() {
    H2 h2 = H2.builder().setSchema("foo").setDriverFile(new File(".")).build();

    assertThat(h2.getSchema()).isNull();
    assertThat(h2.getDriverFile()).isNull();
  }
}
