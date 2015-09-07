/*
 * Orchestrator
 * Copyright (C) 2011 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.orchestrator.build;

import com.google.common.annotations.VisibleForTesting;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.util.ZipUtils;
import com.sonar.orchestrator.version.Version;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * Installs a given version of sonar-runner. It finds the zip into :
 * <ol>
 * <li>the orchestrator distribution</li>
 * <li>maven repositories (local then remote)</li>
 * </ol>
 *
 * @since 2.1
 */
public class SonarRunnerInstaller {
  private static final Logger LOG = LoggerFactory.getLogger(SonarRunnerInstaller.class);

  private final FileSystem fileSystem;

  public SonarRunnerInstaller(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * Installs an ephemeral sonar-runner and returns the path to the script to execute
   */
  public File install(Version runnerVersion, File toDir) {
    clearCachedSnapshot(runnerVersion, toDir);
    if (!isInstalled(runnerVersion, toDir)) {
      LOG.info("Installing sonar-runner " + runnerVersion);
      doInstall(runnerVersion, toDir);
    }
    return locateInstalledScript(runnerVersion, toDir);
  }

  @VisibleForTesting
  void doInstall(Version runnerVersion, File toDir) {
    File zipFile = locateZip(runnerVersion);

    if (zipFile == null || !zipFile.exists()) {
      throw new IllegalArgumentException("Unsupported sonar-runner version: " + runnerVersion);
    }

    try {
      ZipUtils.unzip(zipFile, toDir);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to unzip sonar-runner " + runnerVersion + " to " + toDir, e);
    }
  }

  private File locateZip(Version runnerVersion) {
    File zipFile = null;
    URL zip = SonarRunnerInstaller.class.getResource("/com/sonar/orchestrator/build/sonar-runner-dist-" + runnerVersion.toString() + ".zip");
    if (zip != null) {
      try {
        // can't unzip directly from jar resource. It has to be copied in a temp directory.
        zipFile = File.createTempFile("sonar-runner-dist-" + runnerVersion, "zip");
        FileUtils.copyURLToFile(zip, zipFile);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to unzip " + zip + " to " + zipFile, e);
      }
    } else {
      LoggerFactory.getLogger(SonarRunnerInstaller.class).info("Searching for sonar-runner " + runnerVersion.toString() + " in maven repositories");
      zipFile = fileSystem.locate(mavenLocation(runnerVersion));
    }
    return zipFile;
  }

  static MavenLocation mavenLocation(Version runnerVersion) {
    String groupId;
    String artifactId;
    if (runnerVersion.isGreaterThanOrEquals("2.5")) {
      groupId = "org.sonarsource.sonar-runner";
      artifactId = "sonar-runner-dist";
    } else if (runnerVersion.isGreaterThan("2.0")) {
      groupId = "org.codehaus.sonar.runner";
      artifactId = "sonar-runner-dist";
    } else {
      groupId = "org.codehaus.sonar-plugins";
      artifactId = "sonar-runner";
    }
    return MavenLocation.builder()
      .setGroupId(groupId)
      .setArtifactId(artifactId)
      .setVersion(runnerVersion)
      .withPackaging("zip")
      .build();
  }

  private static void clearCachedSnapshot(Version runnerVersion, File toDir) {
    File runnerDir = new File(toDir, directoryName(runnerVersion));
    if (runnerVersion.isSnapshot() && runnerDir.exists()) {
      LOG.info("Delete sonar-runner cache: {}", runnerDir);
      FileUtils.deleteQuietly(runnerDir);
    }
  }

  private static boolean isInstalled(Version runnerVersion, File toDir) {
    File runnerDir = new File(toDir, directoryName(runnerVersion));
    if (runnerDir.isDirectory() && runnerDir.exists()) {
      LOG.debug("Sonar runner {} already exists at {}", runnerVersion, runnerDir);
      return true;
    }
    return false;
  }

  private static File locateInstalledScript(Version runnerVersion, File toDir) {
    String filename = SystemUtils.IS_OS_WINDOWS ? "sonar-runner.bat" : "sonar-runner";
    File script = new File(toDir, directoryName(runnerVersion) + "/bin/" + filename);
    if (!script.exists()) {
      throw new IllegalStateException("File does not exist: " + script);
    }
    return script;
  }

  private static String directoryName(Version runnerVersion) {
    return "sonar-runner-" + runnerVersion.toString();
  }
}
