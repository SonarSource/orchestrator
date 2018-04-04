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
package com.sonar.orchestrator.version;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionTest {

  @Test
  public void testEquals() {
    assertThat(Version.create("1.1").equals(Version.create("1.1"))).isTrue();
    assertThat(Version.create("1.1").equals(Version.create("1.1.0"))).isTrue();
    assertThat(Version.create("1.1").equals(Version.create("1.1-beta"))).isFalse();
    assertThat(Version.create("1.1.2").equals(Version.create("1.1.0"))).isFalse();
    assertThat(Version.create("1.0.0").equals("otherclass")).isFalse();
  }

  @Test
  public void testIsGreaterThanOrEquals() {
    assertThat(Version.create("1.1").isGreaterThanOrEquals("1.1")).isTrue();
    assertThat(Version.create("1.1.2").isGreaterThanOrEquals("1.1.0")).isTrue();
    assertThat(Version.create("1.2.3.1010").isGreaterThanOrEquals("1.2.3.1000")).isTrue();
    assertThat(Version.create("1.2.3.1010").isGreaterThanOrEquals("1.2.3.1010")).isTrue();
    assertThat(Version.create("1.2.3.1010").isGreaterThanOrEquals("1.2.3.1020")).isFalse();
    assertThat(Version.create("1.0").isGreaterThanOrEquals("1.1.0")).isFalse();
    assertThat(Version.create("6.7").isGreaterThanOrEquals("6.7-RC2")).isTrue();
    assertThat(Version.create("1.0-SNAPSHOT").isGreaterThanOrEquals("1.0")).isFalse();
    assertThat(Version.create("6.7-RC2").isGreaterThanOrEquals("6.7")).isFalse();
    assertThat(Version.create("6.7-RC2").isGreaterThanOrEquals("6.7-RC1")).isTrue();
    assertThat(Version.create("6.7-RC2").isGreaterThanOrEquals("6.7-RC2")).isTrue();
    assertThat(Version.create("9999.9999.9999.999999").isGreaterThanOrEquals("9999.9999.9999.999999")).isTrue();
    assertThat(Version.create("9999.9999.9999.999999").isGreaterThanOrEquals("9999.9999.9999.999998")).isTrue();
  }

  @Test
  public void testIsGreaterThan() {
    assertThat(Version.create("1.1").isGreaterThan("1.1")).isFalse();
    assertThat(Version.create("1.1.2").isGreaterThan("1.1.0")).isTrue();
    assertThat(Version.create("1.0").isGreaterThan("1.1.0")).isFalse();
  }

  @Test
  public void testIsRelease() {
    assertThat(Version.create("1.1").isRelease()).isTrue();
    assertThat(Version.create("1.1.2-SNAPSHOT").isRelease()).isFalse();
  }

  @Test
  public void testIsSnapshot() {
    assertThat(Version.create("1.1").isSnapshot()).isFalse();
    assertThat(Version.create("1.1.2-SNAPSHOT").isSnapshot()).isTrue();
  }

  @Test
  public void test_toString() {
    Version underTest = new Version("1.2.3.4");
    assertThat(underTest.asString()).isEqualTo("1.2.3.4");

    underTest = new Version("1.2");
    assertThat(underTest.asString()).isEqualTo("1.2");

    underTest = new Version("1.2.3");
    assertThat(underTest.asString()).isEqualTo("1.2.3");

    underTest = new Version("1.2.3.456789");
    assertThat(underTest.asString()).isEqualTo("1.2.3.456789");

    underTest = new Version("9999.9999.9999.999999");
    assertThat(underTest.asString()).isEqualTo("9999.9999.9999.999999");
  }
}
