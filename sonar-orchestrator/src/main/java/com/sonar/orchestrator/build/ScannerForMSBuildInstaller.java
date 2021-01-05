/*
 * Orchestrator
 * Copyright (C) 2011-2021 SonarSource SA
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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.build.ScannerForMSBuild.DOT_NET_CORE_INTRODUCTION_MAJOR_VERSION;
import static com.sonar.orchestrator.build.ScannerForMSBuild.DOT_NET_CORE_INTRODUCTION_MINOR_VERSION;
import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;

/**
 * Installs a given version of Scanner for MSBuild. It finds the zip into local maven repository
 *
 * @since 3.13
 */
public class ScannerForMSBuildInstaller {
  public static final String DEFAULT_SCANNER_VERSION = "4.10.0.19059";

  private static final Logger LOG = LoggerFactory.getLogger(ScannerForMSBuildInstaller.class);
  public static final String NETCOREAPP_2_0 = "netcoreapp2.0";
  public static final String NET_46 = "net46";

  private final Locators locators;

  public ScannerForMSBuildInstaller(Locators locators) {
    this.locators = locators;
  }

  /**
   * Installs an ephemeral Scanner for MSBuild and returns the path to the exe to execute
   */
  public File install(@Nullable Version scannerVersion, @Nullable Location location, File toDir, boolean useDotNetCore) {
    if (location != null) {
      return installFromLocation(location, toDir, useDotNetCore);
    } else if (scannerVersion != null) {
      return install(scannerVersion, toDir, useDotNetCore);
    } else {
      return install(Version.create(DEFAULT_SCANNER_VERSION), toDir, useDotNetCore);
    }
  }

  private File installFromLocation(Location location, File toDir, boolean useDotNetCore) {
    clearCachedSnapshot(null, toDir, useDotNetCore);
    if (!isInstalled(null, toDir, useDotNetCore)) {
      LOG.info("Installing Scanner for MSBuild from {}", location);
      File zipFile = locators.locate(location);

      if (zipFile == null || !zipFile.exists()) {
        throw new IllegalArgumentException("File doesn't exist: " + zipFile);
      }
      doInstall(zipFile, toDir, null, useDotNetCore);
    }
    return locateInstalledScript(null, toDir, useDotNetCore);
  }

  private File install(Version scannerVersion, File toDir, boolean useDotNetCore) {
    clearCachedSnapshot(scannerVersion, toDir, useDotNetCore);
    if (!isInstalled(scannerVersion, toDir, useDotNetCore)) {
      LOG.info("Installing Scanner for MSBuild {}", scannerVersion);
      File zipFile = locateZip(scannerVersion, useDotNetCore);

      if (zipFile == null || !zipFile.exists()) {
        throw new IllegalArgumentException("Unsupported scanner for MSBuild version: " + scannerVersion);
      }

      doInstall(zipFile, toDir, scannerVersion, useDotNetCore);
    }
    return locateInstalledScript(scannerVersion, toDir, useDotNetCore);
  }

  void doInstall(File zipFile, File toDir, @Nullable Version scannerVersion, boolean useDotNetCore) {
    try {
      File scannerDir = new File(toDir, directoryName(scannerVersion, useDotNetCore));
      scannerDir.mkdirs();
      ZipUtils.unzip(zipFile, scannerDir);
      if (SystemUtils.IS_OS_UNIX &&
        scannerVersion != null
        && scannerVersion.isGreaterThan(DOT_NET_CORE_INTRODUCTION_MAJOR_VERSION, DOT_NET_CORE_INTRODUCTION_MINOR_VERSION)) {
        // change permissions on binary files from sonar-scanner included in scanner
        setSonarScannerBinariesAsExecutable(scannerDir);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to unzip Scanner for MSBuild from " + zipFile + " to " + toDir, e);
    }
  }

  private static void setSonarScannerBinariesAsExecutable(File scannerDir) {
    File[] sonarScannerDirs = scannerDir.listFiles(file -> file.isDirectory() && file.getName().startsWith("sonar-scanner-"));
    checkState(sonarScannerDirs.length > 0, "sonar-scanner folder not found");
    checkState(sonarScannerDirs.length == 1, "sonar-scanner folder is not unique");
    File binaries = new File(sonarScannerDirs[0], "bin");
    for (File binary : binaries.listFiles()) {
      try {
        Files.setPosixFilePermissions(binary.toPath(), PosixFilePermissions.fromString("rwxrwxrwx"));
      } catch (IOException e) {
        LoggerFactory.getLogger(ScannerForMSBuildInstaller.class).info("Unable to set execute permission to file {}.", binary.getAbsolutePath());
      }
    }

  }

  private File locateZip(Version scannerVersion, boolean useDotNetCore) {
    String zipFileName = String.format("sonar-scanner-msbuild-%s", scannerVersion.toString());
    zipFileName = useDotNetCore ? String.format("%s-%s", zipFileName, NETCOREAPP_2_0) : String.format("%s-%s", zipFileName, NET_46);
    File zipFile = null;
    URL zip = ScannerForMSBuildInstaller.class.getResource(String.format("/com/sonar/orchestrator/build/%s%s", zipFileName, ".zip"));
    if (zip != null) {
      try {
        // can't unzip directly from jar resource. It has to be copied in a temp directory.
        zipFile = File.createTempFile(zipFileName, "zip");
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
    if (scannerVersion.isGreaterThanOrEquals(DOT_NET_CORE_INTRODUCTION_MAJOR_VERSION, DOT_NET_CORE_INTRODUCTION_MINOR_VERSION)) {
      location.setClassifier(getClassifier(useDotNetCore));
    }
    return location.build();
  }

  private static String getClassifier(boolean useDotNetCore) {
    return useDotNetCore ? NETCOREAPP_2_0 : NET_46;
  }

  private static void clearCachedSnapshot(@Nullable Version scannerVersion, File toDir, boolean useDotNetCore) {
    File scannerDir = new File(toDir, directoryName(scannerVersion, useDotNetCore));
    if ((scannerVersion == null || scannerVersion.isSnapshot()) && scannerDir.exists()) {
      LOG.info("Delete Scanner for MSBuild cache: {}", scannerDir);
      FileUtils.deleteQuietly(scannerDir);
    }
  }

  private static boolean isInstalled(@Nullable Version scannerVersion, File toDir, boolean useDotNetCore) {
    File scannerDir = new File(toDir, directoryName(scannerVersion, useDotNetCore));
    if (scannerDir.isDirectory() && scannerDir.exists()) {
      LOG.debug("SonarScanner for MSBuild {} already exists at {}", scannerVersion, scannerDir);
      return true;
    }
    return false;
  }

  private static File locateInstalledScript(@Nullable Version scannerVersion, File toDir, boolean useDotNetCore) {
    String filename = "SonarScanner.MSBuild.exe";
    if (useDotNetCore) {
      filename = "SonarScanner.MSBuild.dll";
    }
    File script = new File(toDir, directoryName(scannerVersion, useDotNetCore) + File.separator + filename);
    if (!script.exists()) {
      throw new IllegalStateException("File does not exist: " + script);
    }
    return script;
  }

  private static String directoryName(@Nullable Version scannerVersion, boolean useDotNetCore) {
    if (scannerVersion != null) {
      return "sonar-scanner-msbuild-" + scannerVersion.toString() + "-" + getClassifier(useDotNetCore);
    } else {
      return "sonar-scanner-msbuild";
    }
  }
}
