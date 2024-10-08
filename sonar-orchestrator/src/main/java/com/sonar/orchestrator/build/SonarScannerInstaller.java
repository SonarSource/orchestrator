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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.locator.Locators;
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
  public static final String GROUP_ID = "org.sonarsource.scanner.cli";
  public static final String ARTIFACT_ID = "sonar-scanner-cli";

  private final Locators locators;

  public SonarScannerInstaller(Locators locators) {
    this.locators = locators;
  }

  /**
   * Installs an ephemeral sonar-scanner-cli and returns the path to the script to execute
   */
  public File install(Version scannerVersion, File toDir) {
    return install(scannerVersion, null, toDir);
  }

  public File install(Version scannerVersion, @Nullable String classifier, File toDir) {
    if (!scannerVersion.isGreaterThanOrEquals(2, 5)) {
      throw new IllegalArgumentException("Unsupported sonar-scanner version: " + scannerVersion);
    }
    clearCachedSnapshot(scannerVersion, classifier, toDir);
    if (!isInstalled(scannerVersion, classifier, toDir)) {
      LOG.info("Installing SonarScanner CLI {}", scannerVersion);
      doInstall(scannerVersion, classifier, toDir);
    }
    return locateInstalledScript(scannerVersion, classifier, toDir);
  }

  void doInstall(Version scannerVersion, @Nullable String classifier, File toDir) {
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
    String cl = classifier == null ? "" : ("-" + classifier);
    URL zip = SonarScannerInstaller.class.getResource("/com/sonar/orchestrator/build/sonar-scanner-cli-" + scannerVersion + cl + ".zip");
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
      zipFile = locators.locate(mavenLocation(scannerVersion, classifier));
    }
    return zipFile;
  }

  static MavenLocation mavenLocation(Version scannerVersion) {
    return mavenLocation(scannerVersion, null);
  }

  static MavenLocation mavenLocation(Version scannerVersion, @Nullable String classifier) {
    MavenLocation.Builder builder = MavenLocation.builder()
      .setGroupId(GROUP_ID)
      .setArtifactId(ARTIFACT_ID)
      .setVersion(scannerVersion.toString())
      .withPackaging("zip");
    if (classifier != null) {
      builder.setClassifier(classifier);
    }
    return builder.build();
  }

  private static void clearCachedSnapshot(Version runnerVersion, @Nullable String classifier, File toDir) {
    File runnerDir = new File(toDir, directoryName(runnerVersion, classifier));
    if (runnerVersion.isSnapshot() && runnerDir.exists()) {
      LOG.info("Delete sonar-scanner cache: {}", runnerDir);
      FileUtils.deleteQuietly(runnerDir);
    }
  }

  private static boolean isInstalled(Version runnerVersion, @Nullable String classifier, File toDir) {
    File runnerDir = new File(toDir, directoryName(runnerVersion, classifier));
    if (runnerDir.isDirectory() && runnerDir.exists()) {
      LOG.debug("SonarQube Scanner {} already exists at {}", runnerVersion, runnerDir);
      return true;
    }
    return false;
  }

  private static File locateInstalledScript(Version scannerVersion, @Nullable String classifier, File toDir) {
    File script = new File(toDir, directoryName(scannerVersion, classifier) + "/bin/" + scriptName());
    if (!script.exists()) {
      throw new IllegalStateException("File does not exist: " + script);
    }
    return script;
  }

  private static String directoryName(Version scannerVersion, @Nullable String classifier) {
    return "sonar-scanner-" + scannerVersion + suffix(classifier);
  }

  private static String suffix(@Nullable String classifier) {
    return classifier == null ? "" : ("-" + classifier);
  }

  private static String scriptName() {
    String basename = "sonar-scanner";
    if (SystemUtils.IS_OS_WINDOWS) {
      return basename + ".bat";
    }
    return basename;
  }

}
