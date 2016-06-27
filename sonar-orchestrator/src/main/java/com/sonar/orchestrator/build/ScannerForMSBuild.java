/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Executes the scanner for MSBuild.
 *
 * @since 3.13
 */
public class ScannerForMSBuild extends Build<ScannerForMSBuild> {

  public static final String DEFAULT_SCANNER_VERSION = "2.1";

  private Version scannerVersion = Version.create(DEFAULT_SCANNER_VERSION);
  private File projectDir;
  private boolean debugLogs = false;
  private boolean useOldRunnerScript = false;
  private String projectKey;
  private String projectName;
  private String projectVersion;

  ScannerForMSBuild() {
  }

  public Version scannerVersion() {
    return scannerVersion;
  }

  public void setUseOldRunnerScript(boolean useOldRunnerScript) {
    this.useOldRunnerScript = useOldRunnerScript;
  }

  public boolean isUseOldRunnerScript() {
    return !scannerVersion().isGreaterThanOrEquals("2.2") || useOldRunnerScript;
  }

  public File getProjectDir() {
    return projectDir;
  }

  public boolean isDebugLogs() {
    return debugLogs;
  }

  public ScannerForMSBuild setScannerVersion(String s) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(s), "version must be set");
    this.scannerVersion = Version.create(s);
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
    Preconditions.checkNotNull(dir, "Project directory must be set");
    Preconditions.checkArgument(dir.exists(), "Project directory must exist");
    Preconditions.checkArgument(dir.isDirectory(), "Project directory must be... a directory");
  }

}
