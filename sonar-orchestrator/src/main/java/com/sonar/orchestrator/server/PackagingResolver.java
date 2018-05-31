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
package com.sonar.orchestrator.server;

import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class PackagingResolver {

  private static final String PRIVATE_GROUP_ID = "com.sonarsource.sonarqube";
  private static final String PUBLIC_GROUP_ID = "org.sonarsource.sonarqube";
  private static final Pattern ZIP_VERSION_PATTERN = Pattern.compile("^.*-(\\d++.*)\\.zip$");

  private final Locators locators;

  public PackagingResolver(Locators locators) {
    this.locators = locators;
  }

  public Packaging resolve(SonarDistribution distribution) {
    Version version = resolveVersion(distribution);
    Optional<File> zipOpt = distribution.getZipFile();
    if (zipOpt.isPresent()) {
      return new Packaging(distribution.getEdition(), version, zipOpt.get());
    }

    String groupId;
    String artifactId;
    if (distribution.getEdition().equals(Edition.COMMUNITY) || !version.isGreaterThanOrEquals(7, 2)) {
      groupId = PUBLIC_GROUP_ID;
      artifactId = "sonar-application";
    } else {
      switch (distribution.getEdition()) {
        case DEVELOPER:
          groupId = PRIVATE_GROUP_ID;
          artifactId = "sonarqube-developer";
          break;
        case ENTERPRISE:
          groupId = PRIVATE_GROUP_ID;
          artifactId = "sonarqube-enterprise";
          break;
        case DATACENTER:
          groupId = PRIVATE_GROUP_ID;
          artifactId = "sonarqube-datacenter";
          break;
        default:
          throw new IllegalStateException("Unknown SonarQube edition : " + distribution.getEdition());
      }
    }

    File zip = locators.locate(newMavenLocationOfZip(groupId, artifactId, version.toString()));
    if (zip == null) {
      throw new IllegalStateException(format("SonarQube %s not found", version));
    }
    return new Packaging(distribution.getEdition(), version, zip);
  }

  private Version resolveVersion(SonarDistribution distribution) {
    Optional<File> zip = distribution.getZipFile();
    if (zip.isPresent()) {
      return guessVersionFromZipName(zip.get());
    }

    Optional<String> versionOrAlias = distribution.getVersion();
    if (!versionOrAlias.isPresent()) {
      throw new IllegalStateException("Missing SonarQube version");
    }

    String version = versionOrAlias.get();
    if (version.contains("LTS") || version.contains("DEV") || version.contains("LATEST_RELEASE") || version.contains("DOGFOOD")) {
      MavenLocation location = newMavenLocationOfZip(PUBLIC_GROUP_ID, "sonar-application", version);
      Optional<String> resolvedVersion = locators.maven().resolveVersion(location);
      if (!resolvedVersion.isPresent()) {
        throw new IllegalStateException("Version can not be resolved: " + location);
      }
      return Version.create(resolvedVersion.get());
    }

    return Version.create(version);
  }

  private static Version guessVersionFromZipName(File zip) {
    Matcher matcher = ZIP_VERSION_PATTERN.matcher(zip.getName());

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Fail to extract version from filename: " + zip.getName());
    }
    return Version.create(matcher.group(1));
  }

  private static MavenLocation newMavenLocationOfZip(String groupId, String artifactId, String version) {
    return MavenLocation.builder()
      .setGroupId(groupId)
      .setArtifactId(artifactId)
      .setVersion(version)
      .withPackaging("zip")
      .build();
  }
}
