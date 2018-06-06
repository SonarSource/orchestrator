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
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackagingResolver {

  private static final String PRIVATE_GROUP_ID = "com.sonarsource.sonarqube";
  private static final String PUBLIC_GROUP_ID = "org.sonarsource.sonarqube";
  private static final Pattern ZIP_VERSION_PATTERN = Pattern.compile("^.*-(\\d++.*)\\.zip$");

  private final Locators locators;

  public PackagingResolver(Locators locators) {
    this.locators = locators;
  }

  public Packaging resolve(SonarDistribution distribution) {
    File zip;

    Optional<Location> location = distribution.getZipLocation();
    if (location.isPresent()) {
      zip = locators.locate(location.get());
      if (zip == null || !zip.exists()) {
        throw new IllegalStateException(String.format("SonarQube not found at %s", location.get()));
      }
    } else {
      Version version = resolveVersion(distribution.getVersion());
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
      MavenLocation mavenLocation = newMavenLocationOfZip(groupId, artifactId, version.toString());
      zip = locators.locate(mavenLocation);
      if (zip == null || !zip.exists()) {
        throw new IllegalStateException(String.format("SonarQube %s not found: %s", distribution.getVersion().get(), mavenLocation));
      }
    }

    return new Packaging(distribution.getEdition(), guessVersionFromZipName(zip), zip);
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

  private Version resolveVersion(Optional<String> versionOrAlias) {
    if (!versionOrAlias.isPresent()) {
      throw new IllegalStateException("Missing SonarQube version");
    }

    String version = versionOrAlias.get();
    if (version.startsWith("DEV") || version.startsWith("LATEST_RELEASE") || version.startsWith("DOGFOOD")) {
      MavenLocation location = newMavenLocationOfZip(PUBLIC_GROUP_ID, "sonar-application", version);
      Optional<String> resolvedVersion = locators.maven().resolveVersion(location);
      if (!resolvedVersion.isPresent()) {
        throw new IllegalStateException("Version can not be resolved: " + location);
      }
      return Version.create(resolvedVersion.get());
    }

    return Version.create(version);
  }
}
