/*
 * Orchestrator
 * Copyright (C) 2011-2017 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.util.ZipUtils;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.net.URL;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs a given version of sonar-scanner. It search the zip into :
 * <ol>
 * <li>the orchestrator distribution</li>
 * <li>local maven repository</li>
 * </ol>
 *
 * @since 2.1
 */
public class SonarScannerInstaller {
  private static final Logger LOG = LoggerFactory.getLogger(SonarScannerInstaller.class);

  private final FileSystem fileSystem;

  public SonarScannerInstaller(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * Installs an ephemeral sonar-runner and returns the path to the script to execute
   */
  public File install(Version scannerVersion, File toDir, boolean useOldScript) {
    return install(scannerVersion, toDir, useOldScript, null);
  }

  public File install(Version scannerVersion, File toDir, boolean useOldScript, @Nullable String classifier) {
    clearCachedSnapshot(scannerVersion, toDir);
    if (!isInstalled(scannerVersion, toDir)) {
      LOG.info("Installing sonar-scanner {}", scannerVersion);
      doInstall(scannerVersion, toDir, classifier);
    }
    return locateInstalledScript(scannerVersion, toDir, useOldScript);
  }

  @VisibleForTesting
  void doInstall(Version scannerVersion, File toDir) {
    doInstall(scannerVersion, toDir, null);
  }

  @VisibleForTesting
  void doInstall(Version scannerVersion, File toDir, @Nullable String classifier) {
    File zipFile = locateZip(scannerVersion, classifier);

    if (zipFile == null || !zipFile.exists()) {
      throw new IllegalArgumentException("Unsupported sonar-scanner version: " + scannerVersion);
    }

    try {
      ZipUtils.unzip(zipFile, toDir);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to unzip sonar-scanner " + scannerVersion + " to " + toDir, e);
    }
  }

  private File locateZip(Version scannerVersion, @Nullable String classifier) {
    File zipFile = null;
    String cl = (classifier == null) ? "" : "-" + classifier;
    URL zip = SonarScannerInstaller.class.getResource("/com/sonar/orchestrator/build/sonar-scanner-" + scannerVersion.toString() + cl + ".zip");
    if (zip != null) {
      try {
        // can't unzip directly from jar resource. It has to be copied in a temp directory.
        zipFile = File.createTempFile("sonar-scanner-" + scannerVersion, "zip");
        FileUtils.copyURLToFile(zip, zipFile);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to unzip " + zip + " to " + zipFile, e);
      }
    } else {
      LoggerFactory.getLogger(SonarScannerInstaller.class).info("Searching for sonar-scanner {} in local maven repository", scannerVersion);
      zipFile = fileSystem.locate(mavenLocation(scannerVersion, classifier));
    }
    return zipFile;
  }

  static MavenLocation mavenLocation(Version scannerVersion) {
    return mavenLocation(scannerVersion, null);
  }

  static MavenLocation mavenLocation(Version scannerVersion, @Nullable String classifier) {
    String groupId;
    String artifactId;
    if (scannerVersion.isGreaterThanOrEquals("2.5")) {
      groupId = "org.sonarsource.scanner.cli";
      artifactId = "sonar-scanner-cli";
    } else if (scannerVersion.isGreaterThan("2.0")) {
      groupId = "org.codehaus.sonar.runner";
      artifactId = "sonar-runner-dist";
    } else {
      groupId = "org.codehaus.sonar-plugins";
      artifactId = "sonar-runner";
    }
    MavenLocation.Builder builder = MavenLocation.builder()
      .setGroupId(groupId)
      .setArtifactId(artifactId)
      .setVersion(scannerVersion)
      .withPackaging("zip");
    if (classifier != null) {
      builder.setClassifier(classifier);
    }
    return builder.build();
  }

  private static void clearCachedSnapshot(Version runnerVersion, File toDir) {
    File runnerDir = new File(toDir, directoryName(runnerVersion));
    if (runnerVersion.isSnapshot() && runnerDir.exists()) {
      LOG.info("Delete sonar-scanner cache: {}", runnerDir);
      FileUtils.deleteQuietly(runnerDir);
    }
  }

  private static boolean isInstalled(Version runnerVersion, File toDir) {
    File runnerDir = new File(toDir, directoryName(runnerVersion));
    if (runnerDir.isDirectory() && runnerDir.exists()) {
      LOG.debug("SonarQube Scanner {} already exists at {}", runnerVersion, runnerDir);
      return true;
    }
    return false;
  }

  private static File locateInstalledScript(Version runnerVersion, File toDir, boolean useOldScript) {
    String filename;
    if (useOldScript) {
      filename = SystemUtils.IS_OS_WINDOWS ? "sonar-runner.bat" : "sonar-runner";
    } else {
      filename = SystemUtils.IS_OS_WINDOWS ? "sonar-scanner.bat" : "sonar-scanner";
    }
    File script = new File(toDir, directoryName(runnerVersion) + "/bin/" + filename);
    if (!script.exists()) {
      throw new IllegalStateException("File does not exist: " + script);
    }
    return script;
  }

  private static String directoryName(Version runnerVersion) {
    if (runnerVersion.isGreaterThanOrEquals("2.5")) {
      return "sonar-scanner-" + runnerVersion.toString();
    } else {
      return "sonar-runner-" + runnerVersion.toString();
    }
  }
}
