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

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.Location;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.checkState;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Executes the scanner for MSBuild.
 *
 * @since 3.13
 */
public class ScannerForMSBuild extends Build<ScannerForMSBuild> {
  public static final String DOT_NET_CORE_INTRODUCTION_VERSION = "4.1.0.1148";
  private Version scannerVersion = null;
  private File projectDir;
  private File dotNetCoreExecutable = null;
  private boolean debugLogs = false;
  private boolean useOldRunnerScript = false;
  private boolean useDotnetCore = false;
  private String projectKey;
  private String projectName;
  private String projectVersion;
  private Location location;

  ScannerForMSBuild() {
  }

  @CheckForNull
  public Version scannerVersion() {
    return scannerVersion;
  }

  public ScannerForMSBuild setUseOldRunnerScript(boolean useOldRunnerScript) {
    this.useOldRunnerScript = useOldRunnerScript;
    return this;
  }

  public boolean isUseOldRunnerScript() {
    if (scannerVersion == null) {
      return useOldRunnerScript;
    }

    return !scannerVersion.isGreaterThanOrEquals("2.2") || useOldRunnerScript;
  }

  public ScannerForMSBuild setUseDotNetCore(boolean useDotnetCore) {
    if (useDotnetCore && scannerVersion != null) {
      checkState(scannerVersion.isGreaterThanOrEquals(DOT_NET_CORE_INTRODUCTION_VERSION),
        "Version of ScannerForMSBuild should be higher than or equals to %s to be able to use .Net Core.",
        DOT_NET_CORE_INTRODUCTION_VERSION);
    }
    this.useDotnetCore = useDotnetCore;
    return this;
  }

  public ScannerForMSBuild setDotNetCoreExecutable(File dotNetCoreExecutable) {
    this.dotNetCoreExecutable = dotNetCoreExecutable;
    // automatically enable usage of .Net Core
    return setUseDotNetCore(true);
  }

  public boolean isUsingDotNetCore() {
    if (scannerVersion == null) {
      return useDotnetCore;
    }
    // .Net Core only available starting from ScannerForMSBuild 4.1.0.1148
    return useDotnetCore && scannerVersion.isGreaterThanOrEquals(DOT_NET_CORE_INTRODUCTION_VERSION);
  }

  @CheckForNull
  public File getDotNetCoreExecutable() {
    return dotNetCoreExecutable;
  }

  public File getProjectDir() {
    return projectDir;
  }

  @CheckForNull
  public Location getLocation() {
    return location;
  }

  public boolean isDebugLogs() {
    return debugLogs;
  }

  public ScannerForMSBuild setScannerVersion(String s) {
    checkArgument(!isEmpty(s), "version must be set");
    this.scannerVersion = Version.create(s);
    return this;
  }

  public ScannerForMSBuild setScannerLocation(Location location) {
    requireNonNull(location);
    this.location = location;
    return this;
  }

  public ScannerForMSBuild setProjectDir(File dir) {
    checkProjectDir(dir);
    this.projectDir = dir;
    return this;
  }

  public ScannerForMSBuild setProjectKey(@Nullable String s) {
    this.projectKey = s;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public ScannerForMSBuild setProjectVersion(@Nullable String s) {
    this.projectVersion = s;
    return this;
  }

  public String getProjectVersion() {
    return projectVersion;
  }

  public ScannerForMSBuild setProjectName(@Nullable String s) {
    this.projectName = s;
    return this;
  }

  public String getProjectName() {
    return projectName;
  }

  public ScannerForMSBuild setDebugLogs(boolean b) {
    this.debugLogs = b;
    return this;
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

  @Override
  BuildResult execute(Configuration config, Map<String, String> adjustedProperties) {
    check();
    return new ScannerForMSBuildExecutor().execute(this, config, adjustedProperties);
  }

  void check() {
    checkProjectDir(projectDir);
  }

  private static void checkProjectDir(File dir) {
    requireNonNull(dir, "Project directory must be set");
    checkArgument(dir.exists(), "Project directory must exist");
    checkArgument(dir.isDirectory(), "Project directory must be... a directory");
  }

}
