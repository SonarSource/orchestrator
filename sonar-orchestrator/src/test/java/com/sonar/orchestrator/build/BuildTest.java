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
package com.sonar.orchestrator.build;

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.config.Configuration;
import java.util.Arrays;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildTest {

  @Test
  public void test_set_null_property_value() {
    FakeBuild build = new FakeBuild();
    assertThat(build.getProperties()).isEmpty();

    build.setProperty("foo", "bar");
    assertThat(build.getProperties()).hasSize(1);
    assertThat(build.getProperty("foo")).isEqualTo("bar");

    build.setProperty("foo", null);
    assertThat(build.getProperties()).isEmpty();
  }

  @Test
  public void test_set_array_of_properties() {
    FakeBuild build = new FakeBuild();
    build.setProperties("one", "1", "two", "2");
    assertThat(build.getProperties()).hasSize(2);
    assertThat(build.getProperty("one")).isEqualTo("1");
    assertThat(build.getProperty("two")).isEqualTo("2");
  }

  @Test
  public void clearProperties() {
    FakeBuild build = new FakeBuild();
    build.setProperties(ImmutableMap.of("one", "1", "two", "2"));
    assertThat(build.getProperties()).hasSize(2);
    assertThat(build.getProperty("one")).isEqualTo("1");
    assertThat(build.getProperty("two")).isEqualTo("2");

    build.clearProperties();
    assertThat(build.getProperties()).isEmpty();
  }

  @Test
  public void withoutDynamicAnalysis() {
    FakeBuild build = new FakeBuild();
    build.withoutDynamicAnalysis();
    assertThat(build.getProperty("sonar.dynamicAnalysis")).isEqualTo("false");
  }

  @Test
  public void addArgument() {
    FakeBuild build = new FakeBuild();
    assertThat(build.arguments()).isEmpty();

    build.addArgument("-Xfoo");
    assertThat(build.arguments()).containsExactly("-Xfoo");
  }

  @Test
  public void addArguments() {
    FakeBuild build = new FakeBuild();
    assertThat(build.arguments()).isEmpty();

    build.addArguments("-Xone", "-Xtwo");
    assertThat(build.arguments()).containsOnly("-Xone", "-Xtwo");

    build.addArguments(Arrays.asList("-Xthree", "-Xfour"));
    assertThat(build.arguments()).containsOnly("-Xone", "-Xtwo", "-Xthree", "-Xfour");
  }

  @Test
  public void setArguments() {
    FakeBuild build = new FakeBuild();
    assertThat(build.arguments()).isEmpty();

    build.setArguments(Arrays.asList("-Xone", "-Xtwo"));
    assertThat(build.arguments()).containsOnly("-Xone", "-Xtwo");

    build.setArguments(Arrays.asList("-Xthree", "-Xfour"));
    assertThat(build.arguments()).containsOnly("-Xthree", "-Xfour");
  }

  @Test
  public void environment_variables() {
    FakeBuild build = new FakeBuild();
    assertThat(build.getEnvironmentVariables()).isEmpty();

    build.setEnvironmentVariable("JAVA_OPTS", "-Xmx512m");
    assertThat(build.getEnvironmentVariables())
      .containsOnly(MapEntry.entry("JAVA_OPTS", "-Xmx512m"));

    build.setEnvironmentVariable("EMPTY", null);
    assertThat(build.getEnvironmentVariables())
      .hasSize(2);
    assertThat(build.getEnvironmentVariables().get("EMPTY")).isNull();
  }

  @Test
  public void environment_variable_prefixes() {
    FakeBuild build = new FakeBuild() {
      @Override
      protected Map<String, String> doGetEnvironmentVariablePrefixes() {
        return ImmutableMap.of(
          "FAKE_OPTS", "-x",
          "JAVA_OPTS", "-Xmx512m");
      }
    };
    build.setEnvironmentVariable("JAVA_OPTS", "-Xms3m -Xmx1G");

    assertThat(build.getEnvironmentVariables()).hasSize(1);
    assertThat(build.getEffectiveEnvironmentVariables())
      .containsOnly(
        MapEntry.entry("FAKE_OPTS", "-x"),
        // JVM is supposed to use latest values (-Xmx1G)
        MapEntry.entry("JAVA_OPTS", "-Xmx512m -Xms3m -Xmx1G"));
  }

  static class FakeBuild extends Build<FakeBuild> {
    @Override
    BuildResult execute(Configuration config, Map<String, String> adjustedProperties) {
      return null;
    }
  }

}
