/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.version.Version;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginLocationTest {

  @Test
  public void testCreate() {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "groupId", "artifactId");
    assertThat(location.key()).isEqualTo("clirr");
    assertThat(location.version().toString()).isEqualTo("1.1");

    location = PluginLocation.builder().setKey("clirr").setVersion(Version.create("1.1")).setGroupId("groupId").setArtifactId("artifactId").build();
    assertThat(location.key()).isEqualTo("clirr");
    assertThat(location.version().toString()).isEqualTo("1.1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void keyShouldBeMandatory() {
    PluginLocation.builder().setVersion("1.1").setGroupId("groupId").setArtifactId("artifactId").build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void versionShouldBeMandatory() {
    PluginLocation.builder().setKey("clirr").setGroupId("groupId").setArtifactId("artifactId").build();
  }

  @Test
  public void test_equals_and_hashCode() {
    PluginLocation location1 = PluginLocation.create("clirr", "1.1", "groupId", "artifactId");
    PluginLocation location2 = PluginLocation.create("clirr", "1.1", "groupId", "artifactId");
    assertThat(location1.equals(location1)).isTrue();
    assertThat(location1.equals(location2)).isTrue();
    assertThat(location1.equals(null)).isFalse();
    assertThat(location2.equals(location1)).isTrue();
    assertThat(location1.hashCode()).isEqualTo(location1.hashCode());

    location1 = PluginLocation.create("clirr", "1.1", "groupId", "artifactId");
    location2 = PluginLocation.create("clirr", "1.2", "groupId", "artifactId");
    assertThat(location1.equals(location2)).isFalse();

    location1 = PluginLocation.create("pdfreport", "1.1", "groupId", "artifactId");
    location2 = PluginLocation.create("clirr", "1.1", "groupId", "artifactId");
    assertThat(location1.equals(location2)).isFalse();
  }

  @Test
  public void test_toString() {
    PluginLocation location = PluginLocation.create("clirr", "1.1", "groupId", "artifactId");

    assertThat(location.toString()).isEqualTo("[clirr:1.1:groupId:artifactId]");
  }
}
