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
  public void test_equals() {
    assertThat(Version.create("1.1").equals(Version.create("1.1"))).isTrue();
    assertThat(Version.create("1.1").equals(Version.create("1.1.0"))).isTrue();
    assertThat(Version.create("1.2-M7_2016-03-30").equals(Version.create("1.2-M7_2016-03-30"))).isTrue();
    assertThat(Version.create("2-M8_2016-03").equals(Version.create("2-M8_2016-03"))).isTrue();
    assertThat(Version.create("1.1").equals(Version.create("1.1-beta"))).isFalse();
    assertThat(Version.create("1.1.2").equals(Version.create("1.1.0"))).isFalse();
    assertThat(Version.create("1.2-M7_2016-03-30").equals(Version.create("1.2-M7_2016-02-01"))).isFalse();
    assertThat(Version.create("2-M8_2016-03").equals(Version.create("1-M8_2016-03"))).isFalse();
    assertThat(Version.create("1.0.0").equals("otherclass")).isFalse();
  }

  @Test
  public void test_compareTo() {
    assertThat(Version.create("1.1").compareTo(Version.create("1.1"))).isEqualTo(0);
    assertThat(Version.create("1.1.2").compareTo(Version.create("1.1.0"))).isGreaterThan(0);
    assertThat(Version.create("1.2.3.1010").compareTo(Version.create("1.2.3.1000"))).isGreaterThan(0);
    assertThat(Version.create("1.2.3.1010").compareTo(Version.create("1.2.3.1010"))).isEqualTo(0);
    assertThat(Version.create("1.2.3.1010").compareTo(Version.create("1.2.3.1020"))).isLessThan(0);
    assertThat(Version.create("1.0").compareTo(Version.create("1.1.0"))).isLessThan(0);
    assertThat(Version.create("6.7").compareTo(Version.create("6.7-RC2"))).isGreaterThan(0);
    assertThat(Version.create("1.0-SNAPSHOT").compareTo(Version.create("1.0"))).isLessThan(0);
    assertThat(Version.create("6.7-RC2").compareTo(Version.create("6.7"))).isLessThan(0);
    assertThat(Version.create("6.7-RC2").compareTo(Version.create("6.7-RC1"))).isGreaterThan(0);
    assertThat(Version.create("6.7-RC2").compareTo(Version.create("6.7-RC2"))).isEqualTo(0);
    assertThat(Version.create("1.2").compareTo(Version.create("1.2-M7_2016-03-30"))).isGreaterThan(0);
    assertThat(Version.create("1.1").compareTo(Version.create("1.2-M7_2016-03-30"))).isLessThan(0);
    assertThat(Version.create("9999.9999.9999.999999").compareTo(Version.create("9999.9999.9999.999999"))).isEqualTo(0);
    assertThat(Version.create("9999.9999.9999.999999").compareTo(Version.create("9999.9999.9999.999998"))).isGreaterThan(0);
  }

  @Test
  public void test_isGreaterThan() {
    assertThat(Version.create("7.3").isGreaterThan(7, 3)).isFalse();
    assertThat(Version.create("7.3.0").isGreaterThan(7, 3)).isFalse();
    assertThat(Version.create("7.3.1").isGreaterThan(7, 3)).isFalse();
    assertThat(Version.create("7.3.0.1000").isGreaterThan(7, 3)).isFalse();
    assertThat(Version.create("7.3.1.1000").isGreaterThan(7, 3)).isFalse();
    assertThat(Version.create("7.3-RC1").isGreaterThan(7, 3)).isFalse();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThan(7, 3)).isFalse();

    assertThat(Version.create("7.3").isGreaterThan(8, 2)).isFalse();
    assertThat(Version.create("7.3.0").isGreaterThan(8, 2)).isFalse();
    assertThat(Version.create("7.3.1").isGreaterThan(8, 2)).isFalse();
    assertThat(Version.create("7.3.0.1000").isGreaterThan(8, 2)).isFalse();
    assertThat(Version.create("7.3.1.1000").isGreaterThan(8, 2)).isFalse();
    assertThat(Version.create("7.3-RC1").isGreaterThan(8, 2)).isFalse();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThan(8, 2)).isFalse();

    assertThat(Version.create("7.3").isGreaterThan(7, 2)).isTrue();
    assertThat(Version.create("7.3.0").isGreaterThan(7, 2)).isTrue();
    assertThat(Version.create("7.3.1").isGreaterThan(7, 2)).isTrue();
    assertThat(Version.create("7.3.0.1000").isGreaterThan(7, 2)).isTrue();
    assertThat(Version.create("7.3.1.1000").isGreaterThan(7, 2)).isTrue();
    assertThat(Version.create("7.3-RC1").isGreaterThan(7, 2)).isTrue();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThan(7, 2)).isTrue();

    assertThat(Version.create("7.3").isGreaterThan(6, 4)).isTrue();
    assertThat(Version.create("7.3.0").isGreaterThan(6, 4)).isTrue();
    assertThat(Version.create("7.3.1").isGreaterThan(6, 4)).isTrue();
    assertThat(Version.create("7.3.0.1000").isGreaterThan(6, 4)).isTrue();
    assertThat(Version.create("7.3.1.1000").isGreaterThan(6, 4)).isTrue();
    assertThat(Version.create("7.3-RC1").isGreaterThan(6, 4)).isTrue();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThan(6, 4)).isTrue();
  }

  @Test
  public void test_isGreaterThanOrEquals() {
    assertThat(Version.create("7.3").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3.0").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3.1").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3.0.1000").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3.1.1000").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3-RC1").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThanOrEquals(7, 3)).isTrue();

    assertThat(Version.create("7.3").isGreaterThanOrEquals(8, 2)).isFalse();
    assertThat(Version.create("7.3.0").isGreaterThanOrEquals(8, 2)).isFalse();
    assertThat(Version.create("7.3.1").isGreaterThanOrEquals(8, 2)).isFalse();
    assertThat(Version.create("7.3.0.1000").isGreaterThanOrEquals(8, 2)).isFalse();
    assertThat(Version.create("7.3.1.1000").isGreaterThanOrEquals(8, 2)).isFalse();
    assertThat(Version.create("7.3-RC1").isGreaterThanOrEquals(8, 2)).isFalse();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThanOrEquals(8, 2)).isFalse();

    assertThat(Version.create("7.3").isGreaterThanOrEquals(7, 2)).isTrue();
    assertThat(Version.create("7.3.0").isGreaterThanOrEquals(7, 2)).isTrue();
    assertThat(Version.create("7.3.1").isGreaterThanOrEquals(7, 2)).isTrue();
    assertThat(Version.create("7.3.0.1000").isGreaterThanOrEquals(7, 2)).isTrue();
    assertThat(Version.create("7.3.1.1000").isGreaterThanOrEquals(7, 2)).isTrue();
    assertThat(Version.create("7.3-RC1").isGreaterThanOrEquals(7, 2)).isTrue();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThanOrEquals(7, 2)).isTrue();

    assertThat(Version.create("7.3").isGreaterThanOrEquals(6, 4)).isTrue();
    assertThat(Version.create("7.3.0").isGreaterThanOrEquals(6, 4)).isTrue();
    assertThat(Version.create("7.3.1").isGreaterThanOrEquals(6, 4)).isTrue();
    assertThat(Version.create("7.3.0.1000").isGreaterThanOrEquals(6, 4)).isTrue();
    assertThat(Version.create("7.3.1.1000").isGreaterThanOrEquals(6, 4)).isTrue();
    assertThat(Version.create("7.3-RC1").isGreaterThanOrEquals(6, 4)).isTrue();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThanOrEquals(6, 4)).isTrue();
  }

  @Test
  public void test_isRelease() {
    assertThat(Version.create("1.1").isRelease()).isTrue();
    assertThat(Version.create("1.1.2-SNAPSHOT").isRelease()).isFalse();
  }

  @Test
  public void test_isSnapshot() {
    assertThat(Version.create("1.1").isSnapshot()).isFalse();
    assertThat(Version.create("1.1.2-SNAPSHOT").isSnapshot()).isTrue();
  }

  @Test
  public void test_toString() {
    Version underTest = new Version("1.2.3.4");
    assertThat(underTest.toString()).isEqualTo("1.2.3.4");

    underTest = new Version("1.2");
    assertThat(underTest.toString()).isEqualTo("1.2");

    underTest = new Version("1.2.3");
    assertThat(underTest.toString()).isEqualTo("1.2.3");

    underTest = new Version("1.2.3.456789");
    assertThat(underTest.toString()).isEqualTo("1.2.3.456789");

    underTest = new Version("9999.9999.9999.999999");
    assertThat(underTest.toString()).isEqualTo("9999.9999.9999.999999");

    underTest = new Version("1.2-M7_2016-03-30");
    assertThat(underTest.toString()).isEqualTo("1.2-M7_2016-03-30");

    underTest = new Version("2-M8_2016-03");
    assertThat(underTest.toString()).isEqualTo("2-M8_2016-03");
  }

}
