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
package com.sonar.orchestrator.locator;

import com.sonar.orchestrator.util.MavenVersionResolver;
import java.io.File;
import java.util.Optional;

public class MavenArtifactory extends Artifactory {

  protected MavenArtifactory(File tempDir, String baseUrl) {
    super(tempDir, baseUrl, null, null);
  }

  @Override
  public Optional<String> resolveVersion(MavenLocation location) {
    MavenVersionResolver versionResolver = new MavenVersionResolver.Builder()
      .setBaseUrl(baseUrl)
      .setGroupId(location.getGroupId())
      .setArtifactId(location.getArtifactId())
      .build();
    versionResolver.loadVersions();

    if (location.getVersion().startsWith("LATEST_RELEASE")) {
      return versionResolver.getLatestVersion(extractVersionFromAlias(location.getVersion()));
    } else if (isUnsupportedVersionAlias(location.getVersion())) {
      throw new IllegalStateException("Unsupported version alias for " + location);
    } else {
      return Optional.of(location.getVersion());
    }
  }

  @Override
  public boolean downloadToFile(MavenLocation location, File toFile) {
    Optional<File> tempFile = super.downloadToDir(location, tempDir, null);
    return tempFile.filter(file -> super.moveFile(file, toFile)).isPresent();
  }

  @Override
  public Optional<File> downloadToDir(MavenLocation location, File toDir) {
    return super.downloadToDir(location, toDir, null);
  }

  private static boolean isUnsupportedVersionAlias(String version) {
    return version.startsWith("DEV") || version.startsWith("DOGFOOD") || version.startsWith("LTS") || version.contains("COMPATIBLE");
  }
}
