/*
 * Orchestrator Utils
 * Copyright (C) 2011-2025 SonarSource Sàrl
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
package com.sonar.orchestrator.util;


import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class Version implements Comparable<Version> {

  private static final String GROUP_MAJOR = "major";
  private static final String GROUP_MINOR = "minor";
  private static final String GROUP_PATCH = "patch";
  private static final String GROUP_QUALIFIER = "qualifier";
  private static final String GROUP_BUILD_NUMBER = "buildNumber";
  private static final String REGEX = "(?<" + GROUP_MAJOR + ">\\d+)" +
          "(\\.(?<" + GROUP_MINOR + ">\\d+))?" +
          "(\\.(?<" + GROUP_PATCH + ">\\d+))?" +
          "(-(?<" + GROUP_QUALIFIER + ">[A-Za-z0-9_-]+))?" +
          "(\\.(?<" + GROUP_BUILD_NUMBER + ">\\d+))?";

  private final String asString;
  private final long asNumber;
  private final long asNumberWOBuildNumber;
  @Nullable
  private final String qualifier;
  private final int major;
  private final int minor;
  private final int patch;
  private final int buildNumber;

  Version(String s) {
    Matcher fields = Pattern.compile(REGEX).matcher(s);
    if(!fields.find()) {
      throw new VersionParsingException();
    }

    long l = 0;
    // max representation: 9999.9999.9999.999999

    if(fields.group(GROUP_MAJOR) != null) {
      this.major = Integer.parseInt(fields.group(GROUP_MAJOR));
    } else {
      this.major = 0;
    }
    l += 1_0000_0000_000000L * this.major;

    if(fields.group(GROUP_MINOR) != null) {
      this.minor = Integer.parseInt(fields.group(GROUP_MINOR));
    } else {
      this.minor = 0;
    }
    l += 1_0000_000000L * this.minor;

    if(fields.group(GROUP_PATCH) != null) {
      this.patch = Integer.parseInt(fields.group(GROUP_PATCH));
    } else {
      this.patch = 0;
    }
    l += 1_000000L * this.patch;

    if(fields.group(GROUP_BUILD_NUMBER) != null) {
      this.buildNumber = Integer.parseInt(fields.group(GROUP_BUILD_NUMBER));
    } else {
      this.buildNumber = 0;
    }
    l += this.buildNumber;

    this.asNumberWOBuildNumber = l;
    this.asNumber = this.asNumberWOBuildNumber + this.buildNumber;
    this.asString = s;
    this.qualifier = fields.group(GROUP_QUALIFIER);
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
    int i = Long.compare(asNumberWOBuildNumber, o.asNumberWOBuildNumber);
    if (i == 0) {
      if(Objects.equals(qualifier, o.qualifier)) {
        i = Integer.compare(buildNumber, o.buildNumber);
      } else if(qualifier == null) {
        i = 1;
      } else if(o.qualifier == null) {
        i = -1;
      } else {
        i = qualifier.compareToIgnoreCase(o.qualifier);
      }
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

  public int getBuildNumber() {
    return buildNumber;
  }

  @Nullable
  public String getQualifier() {
    return qualifier;
  }

  public boolean isRelease() {
    return !isSnapshot();
  }

  public boolean isSnapshot() {
    return asString.endsWith("-SNAPSHOT");
  }

  public static class VersionParsingException extends RuntimeException {
    private static final String PARSE_ERR_MSG = "Version string cannot be parsed.";

    VersionParsingException() {
      super(PARSE_ERR_MSG);
    }
  }
}
