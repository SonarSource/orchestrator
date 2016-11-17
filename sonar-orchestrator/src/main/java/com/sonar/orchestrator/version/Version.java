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
package com.sonar.orchestrator.version;

import org.apache.commons.lang.StringUtils;

public class Version implements Comparable<Version> {

  private final String version;
  // Internally we use Version from update center but don't want to expose it
  private final org.sonar.updatecenter.common.Version sonarVersion;

  private Version(String version) {
    this.version = version;
    this.sonarVersion = org.sonar.updatecenter.common.Version.create(version);
  }

  public static Version create(String version) {
    return new Version(version);
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
    return StringUtils.endsWith(version, "-SNAPSHOT");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Version) {
      Version other = (Version) obj;
      return 0 == other.compareTo(this);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.sonarVersion.hashCode();
  }

  @Override
  public String toString() {
    return version;
  }

  @Override
  public int compareTo(Version o) {
    // This is very specific to Orchestrator and ITs. 1.0-SNAPSHOT is supposed equal to 1.0
    org.sonar.updatecenter.common.Version thisRelease = org.sonar.updatecenter.common.Version.create(this.version).removeQualifier();
    org.sonar.updatecenter.common.Version otherRelease = org.sonar.updatecenter.common.Version.create(o.toString()).removeQualifier();
    return thisRelease.compareTo(otherRelease);
  }
}
