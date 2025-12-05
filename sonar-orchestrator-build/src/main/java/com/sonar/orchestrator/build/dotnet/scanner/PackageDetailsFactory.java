/*
 * Orchestrator Build
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
package com.sonar.orchestrator.build.dotnet.scanner;

import com.sonar.orchestrator.build.version.Version;

public class PackageDetailsFactory {
  // Starting with this version the download paths have changed.
  public static final Version PACKAGING_CHANGE_VERSION = Version.create("6.0.0.81631");

  private static boolean hasNewPackageNames(Version scannerVersion) {
    return scannerVersion.isGreaterThanOrEquals(PACKAGING_CHANGE_VERSION.getMajor(), PACKAGING_CHANGE_VERSION.getMinor());
  }

  public PackageDetails create(Version scannerVersion, boolean useDotNetCore) {
    // Depending on the version of the scanner, the file names, the artifactId and the classifiers can be different.
    // For versions 5.x and below, the file names had the following format:
    // - sonar-scanner-msbuild-<ver>-netcoreapp2.0
    // - sonar-scanner-msbuild-<ver>-net46
    // Starting version 6.x, they are:
    // - sonar-scanner-<ver>-net
    // - sonar-scanner-<ver>-net-framework
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
}
