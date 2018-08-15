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

import java.util.Objects;
import org.apache.commons.lang.StringUtils;

public class Version implements Comparable<Version> {

  private final String asString;
  private final long asNumber;
  private final String qualifier;
  private final int major;
  private final int minor;
  private final int patch;

  Version(String s) {
    String[] fields = StringUtils.substringBefore(s, "-").split("\\.");
    long l = 0;
    // max representation: 9999.9999.9999.999999
    this.major = Integer.parseInt(fields[0]);
    l += 1_0000_0000_000000L * this.major;
    if (fields.length > 1) {
      this.minor = Integer.parseInt(fields[1]);
      l += 1_0000_000000L * this.minor;
      if (fields.length > 2) {
        this.patch = Integer.parseInt(fields[2]);
        l += 1_000000L * this.patch;
        if (fields.length > 3) {
          l += Integer.parseInt(fields[3]);
        }
      } else {
        this.patch = 0;
      }
    } else {
      this.minor = 0;
      this.patch = 0;
    }
    this.asNumber = l;
    this.asString = s;
    this.qualifier = s.contains("-") ? StringUtils.substringAfter(s, "-") : "ZZZ";
  }

  public static Version create(String version) {
    return new Version(version);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Version version = (Version) o;
    return asNumber == version.asNumber && Objects.equals(qualifier, version.qualifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(asNumber, qualifier);
  }

  @Override
  public int compareTo(Version o) {
    int i = Long.compare(asNumber, o.asNumber);
    if (i == 0) {
      i = qualifier.compareToIgnoreCase(o.qualifier);
    }
    return i;
  }

  @Override
  public String toString() {
    return asString;
  }

  /**
   * Compares only the two first parts of versions. The following parts, including
   * qualifier like RC or SNAPSHOT, are ignored.
   */
  public boolean isGreaterThan(int major, int minor) {
    if (this.major > major) {
      return true;
    }
    if (this.major == major) {
      return this.minor > minor;
    }
    return false;
  }

  /**
   * Compares only the two first parts of versions. The following parts, including
   * qualifier like RC or SNAPSHOT, are ignored.
   */
  public boolean isGreaterThanOrEquals(int major, int minor) {
    if (this.major > major) {
      return true;
    }
    if (this.major == major) {
      return this.minor >= minor;
    }
    return false;
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public int getPatch() {
    return patch;
  }

  public boolean isRelease() {
    return !isSnapshot();
  }

  public boolean isSnapshot() {
    return asString.endsWith("-SNAPSHOT");
  }
}
