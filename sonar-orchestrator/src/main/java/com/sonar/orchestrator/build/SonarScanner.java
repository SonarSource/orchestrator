/*
 * Orchestrator
 * Copyright (C) 2011-2025 SonarSource SA
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
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.apache.commons.lang.SystemUtils;

import static com.sonar.orchestrator.util.OrchestratorUtils.checkArgument;
import static com.sonar.orchestrator.util.OrchestratorUtils.isEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Executes the SonarScanner CLI.
 *
 * @since 3.8
 */
public class SonarScanner extends Build<SonarScanner> {

  public static final String DEFAULT_SCANNER_VERSION = "7.0.0.4796";
  public static final String PROP_KEY_SOURCE_ENCODING = "sonar.sourceEncoding";

  private Version scannerVersion = Version.create(DEFAULT_SCANNER_VERSION);
  private File projectDir;
  private boolean debugLogs = false;
  private boolean showErrors = true;
  private String classifier = null;

  SonarScanner() {
  }

  /**
   * @since 3.11
   */
  public Version scannerVersion() {
    return scannerVersion;
  }

  public File getProjectDir() {
    return projectDir;
  }

  public boolean isDebugLogs() {
    return debugLogs;
  }

  public boolean isShowErrors() {
    return showErrors;
  }

  /**
   * @since 3.11
   */
  public SonarScanner setScannerVersion(String s) {
    checkArgument(!isEmpty(s), "version must be set");
    this.scannerVersion = Version.create(s);
    return this;
  }

  public SonarScanner setProjectDir(File dir) {
    checkProjectDir(dir);
    this.projectDir = dir;
    return this;
  }

  public SonarScanner setProjectKey(@Nullable String s) {
    return setProperty("sonar.projectKey", s);
  }

  public SonarScanner setProjectVersion(@Nullable String s) {
    return setProperty("sonar.projectVersion", s);
  }

  public SonarScanner setProjectName(@Nullable String s) {
    return setProperty("sonar.projectName", s);
  }

  public SonarScanner setSourceDirs(@Nullable String s) {
    return setProperty("sonar.sources", s);
  }

  public SonarScanner setTestDirs(@Nullable String testDirs) {
    return setProperty("sonar.tests", testDirs);
  }

  public SonarScanner setSourceEncoding(@Nullable String s) {
    return setProperty(PROP_KEY_SOURCE_ENCODING, s);
  }

  public SonarScanner setDebugLogs(boolean b) {
    this.debugLogs = b;
    return this;
  }

  public SonarScanner setShowErrors(boolean b) {
    this.showErrors = b;
    return this;
  }

  public static SonarScanner create() {
    return new SonarScanner();
  }

  public static SonarScanner create(File projectDir, String... keyValueProperties) {
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
    return new SonarScannerExecutor().execute(this, config, adjustedProperties);
  }

  void check() {
    checkProjectDir(projectDir);
  }

  private static void checkProjectDir(File dir) {
    requireNonNull(dir, "Project directory must be set");
    checkArgument(dir.exists(), "Project directory must exist");
    checkArgument(dir.isDirectory(), "Project directory must be... a directory");
  }

  /**
   * @since 3.15 used by SQ Scanner CLI ITs
   */
  public SonarScanner useNative() {
    this.classifier = determineClassifier();
    return this;
  }

  private String determineClassifier() {
    if (scannerVersion.isGreaterThanOrEquals(6, 1)) {
      return determineOs() + "-" + determineArchitecture();
    } else {
      return determineOs();
    }
  }

  private static String determineOs() {
    return determineOs(() -> SystemUtils.IS_OS_LINUX, () -> SystemUtils.IS_OS_WINDOWS, () -> SystemUtils.IS_OS_MAC_OSX);
  }

  static String determineOs(BooleanSupplier isOsLinux, BooleanSupplier isOsWindows, BooleanSupplier isOsMacOsx) {
    if (isOsLinux.getAsBoolean()) {
      return "linux";
    }
    if (isOsWindows.getAsBoolean()) {
      return "windows";
    }
    if (isOsMacOsx.getAsBoolean()) {
      return "macosx";
    }
    throw new IllegalStateException("Unsupported OS: only Linux, Windows and Mac OS X are supported");
  }

  private static String determineArchitecture() {
    return determineArchitecture(() -> SystemUtils.IS_OS_LINUX, () -> SystemUtils.IS_OS_WINDOWS, () -> SystemUtils.IS_OS_MAC_OSX, () -> SystemUtils.OS_ARCH);
  }

  static String determineArchitecture(BooleanSupplier isOsLinux, BooleanSupplier isOsWindows, BooleanSupplier isOsMacOsx, Supplier<String> archSupplier) {
    if (isOsLinux.getAsBoolean() || isOsMacOsx.getAsBoolean()) {
      if (archSupplier.get().equals("aarch64")) {
        return "aarch64";
      } else {
        return "x64";
      }
    }
    if (isOsWindows.getAsBoolean()) {
      return "x64";
    }
    throw new IllegalStateException("Unsupported OS: only Linux, Windows and Mac OS X are supported");
  }

  public String classifier() {
    return classifier;
  }

  public SonarScanner setClassifier(String classifier) {
    this.classifier = classifier;
    return this;
  }
}
