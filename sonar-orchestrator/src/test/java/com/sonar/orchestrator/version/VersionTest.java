/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.orchestrator.version;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VersionTest {

  @Test
  public void test_parsing_failed() {
    assertThatThrownBy(() -> Version.create("test")).isInstanceOf(Version.VersionParsingException.class);
  }

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
  public void test_hashCode() {
    assertThat(Version.create("1.1").hashCode()).isEqualTo(Version.create("1.1").hashCode());
    assertThat(Version.create("1.2.3.4").hashCode()).isEqualTo(Version.create("1.2.3.4").hashCode());
  }

  @Test
  public void test_fields() {
    testFields("7", 7, 0, 0);
    testFields("7.0", 7, 0, 0);
    testFields("7.0.0", 7, 0, 0);
    testFields("7.0.0.1000", 7, 0, 0);
    testFields("7.3", 7, 3, 0);
    testFields("7.3.5", 7, 3, 5);
    testFields("7.3-SNAPSHOT", 7, 3, 0);
    testFields("7.3-RC1", 7, 3, 0);
    testFields("7.3.2-SNAPSHOT", 7, 3, 2);
    testFields("7.3-alpha1", 7, 3, 0);
  }

  @Test
  public void test_additional_fields() {
    Version version = Version.create("1.2.3-M1.1234");
    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(2);
    assertThat(version.getPatch()).isEqualTo(3);
    assertThat(version.getQualifier()).isEqualTo("M1");
    assertThat(version.getBuildNumber()).isEqualTo(1234);

    version = Version.create("1.2-M1.1234");
    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(2);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getQualifier()).isEqualTo("M1");
    assertThat(version.getBuildNumber()).isEqualTo(1234);

    version = Version.create("1.2-beta");
    assertThat(version.getMajor()).isEqualTo(1);
    assertThat(version.getMinor()).isEqualTo(2);
    assertThat(version.getPatch()).isEqualTo(0);
    assertThat(version.getQualifier()).isEqualTo("beta");
    assertThat(version.getBuildNumber()).isEqualTo(0);
  }

  private static void testFields(String version, int expectedMajor, int expectedMinor, int expectedPatch) {
    Version v = Version.create(version);
    assertThat(v.getMajor()).isEqualTo(expectedMajor);
    assertThat(v.getMinor()).isEqualTo(expectedMinor);
    assertThat(v.getPatch()).isEqualTo(expectedPatch);
  }

  @Test
  public void test_compareTo() {
    assertThat(Version.create("1").compareTo(Version.create("1"))).isEqualTo(0);
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

    assertThat(Version.create("7.3-alpha1").compareTo(Version.create("7.3-alpha1"))).isEqualTo(0);
    assertThat(Version.create("7.3-alpha1").compareTo(Version.create("7.3"))).isLessThan(0);
    assertThat(Version.create("7.3-alpha1").compareTo(Version.create("7.3-alpha2"))).isLessThan(0);
    assertThat(Version.create("7.3-alpha1").compareTo(Version.create("7.2"))).isGreaterThan(0);
    assertThat(Version.create("7.3-alpha1").compareTo(Version.create("7.4"))).isLessThan(0);
    assertThat(Version.create("7.3-alpha1").compareTo(Version.create("7.4-RC1"))).isLessThan(0);
  }

  @Test
  public void test_isGreaterThan() {
    assertThat(Version.create("7").isGreaterThan(7, 0)).isFalse();
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
    assertThat(Version.create("7").isGreaterThanOrEquals(7, 0)).isTrue();
    assertThat(Version.create("7.3").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3.0").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3.1").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3.0.1000").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3.1.1000").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3-RC1").isGreaterThanOrEquals(7, 3)).isTrue();
    assertThat(Version.create("7.3-SNAPSHOT").isGreaterThanOrEquals(7, 3)).isTrue();

    assertThat(Version.create("7").isGreaterThanOrEquals(7, 3)).isFalse();
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
