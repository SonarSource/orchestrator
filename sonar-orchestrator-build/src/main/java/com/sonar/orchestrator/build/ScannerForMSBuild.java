/*
 * Orchestrator
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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.GitHub;
import com.sonar.orchestrator.locator.GitHubImpl;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.locator.Locators;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonar.orchestrator.util.Utils.checkArgument;
import static com.sonar.orchestrator.util.Utils.checkState;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Executes the scanner for MSBuild.
 *
 * @since 3.13
 */
public class ScannerForMSBuild extends Build<ScannerForMSBuild> {
  public static final String LATEST_RELEASE = "LATEST_RELEASE";
  // 4.1.0.1148
  static final int DOT_NET_CORE_INTRODUCTION_MAJOR_VERSION = 4;
  static final int DOT_NET_CORE_INTRODUCTION_MINOR_VERSION = 1;
  // as of v11.0.0 of the scanner for .NET, the sonar-scanner CLI is no longer bundled
  static final int DOT_NET_SCANNER_CLI_NOT_BUNDLED_MAJOR_VERSION = 11;
  static final int DOT_NET_SCANNER_CLI_NOT_BUNDLED_MINOR_VERSION = 0;
  public static final String DOT_NET_CORE_INTRODUCTION_VERSION = DOT_NET_CORE_INTRODUCTION_MAJOR_VERSION + "." + DOT_NET_CORE_INTRODUCTION_MINOR_VERSION;
  private static final Logger LOG = LoggerFactory.getLogger(ScannerForMSBuild.class);

  private final GitHub gitHub;
  private Version scannerVersion = null;
  private File projectDir;
  private File dotNetCoreExecutable = null;
  private boolean debugLogs = false;
  private boolean useDotnetCore = false;
  private String projectKey;
  private String projectName;
  private String projectVersion;
  private Location location;

  ScannerForMSBuild() {
    this(new GitHubImpl());
  }

  ScannerForMSBuild(GitHub gitHub) {
    this.gitHub = gitHub;
  }

  private static Optional<String> getLatestScannerVersionFromArtifactory() {
    Configuration config = Configuration.createEnv();
    Locators locators = new Locators(config);
    MavenLocation build = MavenLocation.builder()
      .setGroupId("org.sonarsource.scanner.msbuild")
      .setArtifactId("sonar-scanner")
      .setVersion(LATEST_RELEASE)
      .build();
    return locators.maven().resolveVersion(build);
  }

  public static ScannerForMSBuild create() {
    return new ScannerForMSBuild();
  }

  public static ScannerForMSBuild create(File projectDir, String... keyValueProperties) {
    return
    // default value
    create()
      // incoming values
      .setProjectDir(projectDir)
      .setProperties(keyValueProperties);
  }

  private static void checkDotNetCoreCompatibility(Version scannerVersion, boolean useDotNetCore) {
    if (useDotNetCore) {
      checkArgument(scannerVersion != null, "Default version of SonarScanner for MSBuild embedded by Orchestrator does not support .NET Core. "
        + "Please provide a scanner version >= %s.", DOT_NET_CORE_INTRODUCTION_VERSION);
      checkState(scannerVersion.isGreaterThanOrEquals(DOT_NET_CORE_INTRODUCTION_MAJOR_VERSION, DOT_NET_CORE_INTRODUCTION_MINOR_VERSION),
        "Version of ScannerForMSBuild should be greater than or equals to %s to be able to use .Net Core.",
        DOT_NET_CORE_INTRODUCTION_VERSION);
    }
  }

  private static void checkProjectDir(File dir) {
    requireNonNull(dir, "Project directory must be set");
    checkArgument(dir.exists(), "Project directory must exist");
    checkArgument(dir.isDirectory(), "Project directory must be... a directory");
  }

  @CheckForNull
  public Version scannerVersion() {
    return scannerVersion;
  }

  /**
   * Force usage of .NET Core version of ScannerForMSBuild.
   * Note that if the path to dotnet executable is not provided, orchestrator will assume it is available in PATH.
   *
   * @param useDotnetCore true to use .NET Core version
   * @return The scanner being built
   */
  public ScannerForMSBuild setUseDotNetCore(boolean useDotnetCore) {
    this.useDotnetCore = useDotnetCore;
    return this;
  }

  /**
   * In order to use .NET Core, the provided version of SonarScanner for MSBuild should be higher or
   * equal to {@link ScannerForMSBuild#DOT_NET_CORE_INTRODUCTION_VERSION}.
   *
   * @return true if using .NET Core and provided version of SonarScanner for MSBuild is compatible.
   */
  public boolean isUsingDotNetCore() {
    if (scannerVersion == null) {
      return useDotnetCore;
    }
    // .Net Core only available starting from ScannerForMSBuild 4.1.0.1148
    return useDotnetCore && scannerVersion.isGreaterThanOrEquals(DOT_NET_CORE_INTRODUCTION_MAJOR_VERSION, DOT_NET_CORE_INTRODUCTION_MINOR_VERSION);
  }

  @CheckForNull
  public File getDotNetCoreExecutable() {
    return dotNetCoreExecutable;
  }

  /**
   * Providing a .NET Core executable to be used during analysis, force usage of .NET Core version of the scanner. If
   * not provided and usage of .NET Core is enabled, orchestrator will assume that the dotnet executable is available in PATH.
   *
   * Note that there is no need to call {@link #setUseDotNetCore(boolean)} if manually setting the dotnet executable.
   *
   * @param dotNetCoreExecutable the path to the .NET Core executable
   * @return The scanner being built
   */
  public ScannerForMSBuild setDotNetCoreExecutable(File dotNetCoreExecutable) {
    this.dotNetCoreExecutable = dotNetCoreExecutable;
    return setUseDotNetCore(true);
  }

  public File getProjectDir() {
    return projectDir;
  }

  public ScannerForMSBuild setProjectDir(File dir) {
    checkProjectDir(dir);
    this.projectDir = dir;
    return this;
  }

  @CheckForNull
  public Location getLocation() {
    return location;
  }

  public boolean isDebugLogs() {
    return debugLogs;
  }

  public ScannerForMSBuild setDebugLogs(boolean b) {
    this.debugLogs = b;
    return this;
  }

  public ScannerForMSBuild setScannerVersion(String s) {
    checkArgument(!isEmpty(s), "version must be set");
    if (s.equals(LATEST_RELEASE)) {
      try {
        s = gitHub.getLatestScannerReleaseVersion();
      } catch (IllegalStateException e) {
        s = getLatestScannerVersionFromArtifactory()
          .orElseThrow(() -> new IllegalStateException("Failed to resolve latest version of Scanner for MSBuild"));
        LOG.debug("Failed to retrieve latest version from GitHub, using version from Artifactory", e);
      }
    }
    LOG.info("Setting the scanner version to {}", s);
    this.scannerVersion = Version.create(s);
    return this;
  }

  public ScannerForMSBuild setScannerLocation(Location location) {
    requireNonNull(location);
    this.location = location;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public ScannerForMSBuild setProjectKey(@Nullable String s) {
    this.projectKey = s;
    return this;
  }

  public String getProjectVersion() {
    return projectVersion;
  }

  public ScannerForMSBuild setProjectVersion(@Nullable String s) {
    this.projectVersion = s;
    return this;
  }

  public String getProjectName() {
    return projectName;
  }

  public ScannerForMSBuild setProjectName(@Nullable String s) {
    this.projectName = s;
    return this;
  }

  @Override
  BuildResult execute(Configuration config, Locators locators, Map<String, String> adjustedProperties) {
    check();
    return new ScannerForMSBuildExecutor().execute(this, config, locators, adjustedProperties);
  }

  void check() {
    checkProjectDir(projectDir);
    checkDotNetCoreCompatibility(scannerVersion, useDotnetCore);
  }

}
