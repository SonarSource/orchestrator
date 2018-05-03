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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.util.ZipUtils;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.net.URL;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs a given version of Scanner for MSBuild. It finds the zip into local maven repository
 *
 * @since 3.13
 */
public class ScannerForMSBuildInstaller {
  public static final String DEFAULT_SCANNER_VERSION = "2.2.0.24";
  private static final Logger LOG = LoggerFactory.getLogger(ScannerForMSBuildInstaller.class);

  private final Locators locators;

  public ScannerForMSBuildInstaller(Locators locators) {
    this.locators = locators;
  }

  /**
   * Installs an ephemeral Scanner for MSBuild and returns the path to the exe to execute
   */
  public File install(@Nullable Version scannerVersion, @Nullable Location location, File toDir, boolean useOldScript, boolean useDotNetCore) {
    if (location != null) {
      return install(location, toDir, useOldScript, useDotNetCore);
    } else if (scannerVersion != null) {
      return install(scannerVersion, toDir, useOldScript, useDotNetCore);
    } else {
      return install(Version.create(DEFAULT_SCANNER_VERSION), toDir, useOldScript, useDotNetCore);
    }
  }

  private File install(Location location, File toDir, boolean useOldScript, boolean useDotNetCore) {
    clearCachedSnapshot(null, toDir);
    if (!isInstalled(null, toDir)) {
      LOG.info("Installing Scanner for MSBuild from {}", location);
      File zipFile = locators.locate(location);

      if (zipFile == null || !zipFile.exists()) {
        throw new IllegalArgumentException("File doesn't exist: " + zipFile);
      }
      doInstall(zipFile, toDir, null);
    }
    return locateInstalledScript(null, toDir, useOldScript, useDotNetCore);
  }

  private File install(Version scannerVersion, File toDir, boolean useOldScript, boolean useDotNetCore) {
    clearCachedSnapshot(scannerVersion, toDir);
    if (!isInstalled(scannerVersion, toDir)) {
      LOG.info("Installing Scanner for MSBuild {}", scannerVersion);
      File zipFile = locateZip(scannerVersion, useDotNetCore);

      if (zipFile == null || !zipFile.exists()) {
        throw new IllegalArgumentException("Unsupported scanner for MSBuild version: " + scannerVersion);
      }

      doInstall(zipFile, toDir, scannerVersion);
    }
    return locateInstalledScript(scannerVersion, toDir, useOldScript, useDotNetCore);
  }

  void doInstall(File zipFile, File toDir, @Nullable Version scannerVersion) {
    try {
      File scannerDir = new File(toDir, directoryName(scannerVersion));
      scannerDir.mkdirs();
      ZipUtils.unzip(zipFile, scannerDir);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to unzip Scanner for MSBuild from " + zipFile + " to " + toDir, e);
    }
  }

  private File locateZip(Version scannerVersion, boolean useDotNetCore) {
    File zipFile = null;
    URL zip = ScannerForMSBuildInstaller.class.getResource("/com/sonar/orchestrator/build/sonar-scanner-msbuild-" + scannerVersion.toString() + ".zip");
    if (zip != null) {
      try {
        // can't unzip directly from jar resource. It has to be copied in a temp directory.
        zipFile = File.createTempFile("sonar-scanner-msbuild-" + scannerVersion, "zip");
        FileUtils.copyURLToFile(zip, zipFile);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to unzip " + zip + " to " + zipFile, e);
      }
    } else {
      LoggerFactory.getLogger(ScannerForMSBuildInstaller.class).info("Searching for Scanner for MSBuild {} in maven repository", scannerVersion);
      zipFile = locators.locate(mavenLocation(scannerVersion, useDotNetCore));
    }
    return zipFile;
  }

  static MavenLocation mavenLocation(Version scannerVersion) {
    return mavenLocation(scannerVersion, false);
  }

  static MavenLocation mavenLocation(Version scannerVersion, boolean useDotNetCore) {
    String groupId = "org.sonarsource.scanner.msbuild";
    String artifactId = "sonar-scanner-msbuild";
    MavenLocation.Builder location = MavenLocation.builder()
      .setGroupId(groupId)
      .setArtifactId(artifactId)
      .setVersion(scannerVersion.toString())
      .withPackaging("zip");
    if (scannerVersion.isGreaterThanOrEquals(ScannerForMSBuild.DOT_NET_CORE_INTRODUCTION_VERSION)) {
      location.setClassifier(useDotNetCore ? "netcoreapp2.0" : "net46");
    }
    return location.build();
  }

  private static void clearCachedSnapshot(@Nullable Version scannerVersion, File toDir) {
    File scannerDir = new File(toDir, directoryName(scannerVersion));
    if ((scannerVersion == null || scannerVersion.isSnapshot()) && scannerDir.exists()) {
      LOG.info("Delete Scanner for MSBuild cache: {}", scannerDir);
      FileUtils.deleteQuietly(scannerDir);
    }
  }

  private static boolean isInstalled(Version scannerVersion, File toDir) {
    File scannerDir = new File(toDir, directoryName(scannerVersion));
    if (scannerDir.isDirectory() && scannerDir.exists()) {
      LOG.debug("SonarQube Scanner for MSBuild {} already exists at {}", scannerVersion, scannerDir);
      return true;
    }
    return false;
  }

  private static File locateInstalledScript(@Nullable Version scannerVersion, File toDir, boolean useOldScript, boolean useDotNetCore) {
    String filename;
    if (useOldScript) {
      filename = "MSBuild.SonarQube.Runner.exe";
    } else {
      if (useDotNetCore || (scannerVersion != null && scannerVersion.isGreaterThanOrEquals(ScannerForMSBuild.DOT_NET_CORE_INTRODUCTION_VERSION))) {
        if (useDotNetCore) {
          filename = "SonarScanner.MSBuild.dll";
        } else {
          filename = "SonarScanner.MSBuild.exe";
        }
      } else {
        filename = "SonarQube.Scanner.MSBuild.exe";
      }
    }
    File script = new File(toDir, directoryName(scannerVersion) + "/" + filename);
    if (!script.exists()) {
      throw new IllegalStateException("File does not exist: " + script);
    }
    return script;
  }

  private static String directoryName(@Nullable Version scannerVersion) {
    if (scannerVersion != null) {
      return "sonar-scanner-msbuild-" + scannerVersion.toString();
    } else {
      return "sonar-scanner-msbuild";
    }
  }
}
