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

import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class H2Test {

  @Test
  public void testDefaultConfiguration() {
    H2 h2 = H2.builder().build();

    assertThat(h2.getDialect()).isEqualTo("h2");
    assertThat(h2.getDriverClassName()).isEqualTo("org.h2.Driver");
    assertThat(h2.getUrl()).matches("jdbc:h2:tcp://localhost:[0-9]*/sonar;USER=sonar;PASSWORD=sonar");
    assertThat(h2.getLogin()).isEqualTo("sonar");
    assertThat(h2.getPassword()).isEqualTo("sonar");
    assertThat(h2.isDropAndCreate()).isFalse();
  }

  @Test
  public void ddlShouldNotBeDefined() {
    H2 h2 = H2.builder().build();

    assertThat(h2.getCreateDdl()).isEmpty();
    assertThat(h2.getDropDdl()).isEmpty();
  }

  @Test
  public void shouldSetPortProperty() {
    H2 h2 = H2.builder().build();

    assertThat(h2.getUrl()).matches("jdbc:h2:tcp://localhost:\\d*/sonar;USER=sonar;PASSWORD=sonar");
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
