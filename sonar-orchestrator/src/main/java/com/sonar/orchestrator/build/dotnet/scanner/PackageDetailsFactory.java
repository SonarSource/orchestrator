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
package com.sonar.orchestrator.build.dotnet.scanner;

import com.sonar.orchestrator.version.Version;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class PackageDetailsFactory {
  // Starting with this version the download paths have changed.
  public static final Version PACKAGING_CHANGE_VERSION = Version.create("6.0.0.81631");

  public PackageDetails create(@NotNull Version scannerVersion, boolean useDotNetCore) {
    // Depending on the version of the scanner, the file names, the artifactId and the classifiers can be different.
    // For versions 5.x and below, the file names had the following format:
    //  - sonar-scanner-msbuild-<ver>-netcoreapp2.0
    //  - sonar-scanner-msbuild-<ver>-net46
    // Starting version 6.x, they are:
    //  - sonar-scanner-<ver>-net
    //  - sonar-scanner-<ver>-net-framework
    String executableName = useDotNetCore ? "SonarScanner.MSBuild.dll" : "SonarScanner.MSBuild.exe";
    if (hasNewPackageNames(scannerVersion)) {
      final String classifier = useDotNetCore ? "net" : "net-framework";
      final String filePath = String.format("sonar-scanner-%s-%s", scannerVersion, classifier);
      return new PackageDetails("org.sonarsource.scanner.msbuild", "sonar-scanner", classifier, filePath, executableName);
    } else {
      final String classifier = useDotNetCore ? "netcoreapp2.0" : "net46";
      final String filePath = String.format("sonar-scanner-msbuild-%s-%s", scannerVersion, classifier);
      return new PackageDetails("org.sonarsource.scanner.msbuild", "sonar-scanner-msbuild", classifier, filePath, executableName);
    }
  }

  private static boolean hasNewPackageNames(@Nonnull Version scannerVersion) {
    return scannerVersion.isGreaterThanOrEquals(PACKAGING_CHANGE_VERSION.getMajor(), PACKAGING_CHANGE_VERSION.getMinor());
  }
}
