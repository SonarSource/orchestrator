/*
 * Orchestrator
 * Copyright (C) 2011-2024 SonarSource SA
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

import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.util.Optional;

import static com.sonar.orchestrator.server.PackagingResolver.PUBLIC_GROUP_ID;

public class VersionUtils {

  public static Version getVersion(String version, Locators locators) {
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



  public static MavenLocation newMavenLocationOfZip(String groupId, String artifactId, String version) {
    return MavenLocation.builder()
      .setGroupId(groupId)
      .setArtifactId(artifactId)
      .setVersion(version)
      .withPackaging("zip")
      .build();
  }

}
