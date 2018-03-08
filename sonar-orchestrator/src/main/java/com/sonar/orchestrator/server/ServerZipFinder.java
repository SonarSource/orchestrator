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

import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.container.SonarDistribution;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Optional;

import static java.lang.String.format;

public class ServerZipFinder {

  private final FileSystem fs;

  public ServerZipFinder(FileSystem fs) {
    this.fs = fs;
  }

  /**
   * Warning - name of returned zip file can be inconsistent with requested version, for example
   * {@code find(Version.of("5.4-SNAPSHOT")).getName()} may equal {@code "5.4-build1234"}
   */
  public File find(SonarDistribution distrib) {
    Optional<File> localZip = distrib.getZipFile();
    if (localZip.isPresent()) {
      return localZip.get();
    }
    Version version = distrib.version().orElseThrow(() -> new IllegalStateException("Missing SonarQube version"));
    File zip = fs.locate(MavenLocation.builder()
      .setGroupId("org.sonarsource.sonarqube")
      .setArtifactId("sonar-application")
      .setVersion(version)
      .withPackaging("zip")
      .build());
    if (zip == null) {
      throw new IllegalStateException(format("SonarQube %s not found", version));
    }
    return zip;
  }
}
