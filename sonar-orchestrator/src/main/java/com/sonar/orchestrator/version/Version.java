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

  Version(String s) {
    String[] fields = StringUtils.substringBeforeLast(s, "-").split("\\.");
    long l = 0;
    // max representation: 9999.9999.9999.999999
    if (fields.length > 0) {
      l += 1_0000_0000_000000L * Integer.parseInt(fields[0]);
      if (fields.length > 1) {
        l += 1_0000_000000L * Integer.parseInt(fields[1]);
        if (fields.length > 2) {
          l += 1_000000L * Integer.parseInt(fields[2]);
          if (fields.length > 3) {
            l += Integer.parseInt(fields[3]);
          }
        }
      }
    }
    this.asNumber = l;
    this.asString = s;
    this.qualifier = s.contains("-") ? StringUtils.substringAfterLast(s, "-") : "ZZZ";
  }

  public static Version create(String version) {
    return new Version(version);
  }

  String asString() {
    return asString;
  }

  long asNumber() {
    return asNumber;
  }

  String qualifier() {
    return qualifier;
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
    if (i ==0) {
      i = qualifier.compareTo(o.qualifier);
    }
    return i;
  }

  @Override
  public String toString() {
    return asString;
  }

  public boolean isGreaterThan(String other) {
    return isGreaterThan(new Version(other));
  }

  public boolean isGreaterThan(Version other) {
    return this.compareTo(other) > 0;
  }

  public boolean isGreaterThanOrEquals(String other) {
    return isGreaterThanOrEquals(new Version(other));
  }

  public boolean isGreaterThanOrEquals(Version other) {
    return this.compareTo(other) >= 0;
  }

  public boolean isRelease() {
    return !isSnapshot();
  }

  public boolean isSnapshot() {
    return asString.endsWith("-SNAPSHOT");
  }
}
